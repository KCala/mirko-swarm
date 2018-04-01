package kcala.mirkoSwarm.wykop

import akka.NotUsed
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import akka.stream.{Graph, SourceShape}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.json.JsonSupport
import kcala.mirkoSwarm.main.Deps
import kcala.mirkoSwarm.model.{Entry, SwarmError}
import kcala.mirkoSwarm.wykop.WykopApiHandler.{WykopApiException, _}

import scala.collection.immutable.Seq
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}

class WykopApiHandler(wykopApiHost: String, wykopApiKey: String, interval: FiniteDuration, waitOnFail: FiniteDuration)(implicit deps: Deps) extends JsonSupport with StrictLogging {

  import deps._

  private val mirkoEntriesEndpoint = s"/stream/index/appkey,$wykopApiKey"

  private val connectionPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](wykopApiHost)

  /**
    * This flow outputs a sequence of entries from Wykop.pl API on every ping
    * The size of returned sequence is equal to the number of entries returned from API (around 50)
    * It is almost sure that there will be duplicate entries between subsequent API calls
    */
  private val wykopEntriesFetchFlow: Flow[RestRequest, Seq[MirkoEntry], NotUsed] = Flow[RestRequest]
    .map(_ => HttpRequest(uri = Uri(mirkoEntriesEndpoint)) -> RedundantInt)
    .via(connectionPool)
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        val message = "Error connecting to wykop"
        logger.error(s"$message: $ex")
        throw new WykopApiException(SwarmError(message), message, ex)
    }
    .mapAsync(10)(resp => {
      Unmarshal(resp.entity).to[Either[MirkoError, Seq[MirkoEntry]]]
        .map {
          case Right(entries) => entries
          case Left(error) => throw new WykopApiException(MirkoError.convertToSwarmError(error), error.message)
        }
    }
    )
    .map(_.reverse)

  val entriesSource: Source[Either[SwarmError, Entry], Cancellable] =
    tickedEntriesSource(0.seconds, interval)
      .map(Right(_))
      .recoverWithRetries(InfiniteAttempts, {
        case ex: WykopApiException => singleErrorAndThenEntriesSource(ex.swarmError)
      })

  private def singleErrorAndThenEntriesSource(swarmError: SwarmError): Source[Either[SwarmError, Entry], NotUsed] = {
    logger.info(s"Sending error to clients. Will retry to reconnect to wykop in $waitOnFail")
    val graph: Graph[SourceShape[Either[SwarmError, Entry]], NotUsed] = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val entriesSource = tickedEntriesSource(waitOnFail, interval).mapMaterializedValue(_ => NotUsed).map(Right(_))
      //TODO this shouldn't be single but on every tick so clients know what's up. However drop it after first successfull entry
      val errorSource = Source.tick(0.seconds, interval, Left(swarmError))
      val merge = b.add(Merge[Either[SwarmError, Entry]](2))

      val filter = b.add(Flow[Either[SwarmError, Entry]].statefulMapConcat(() => {
        var properEntryAppeared = false
        either => {
          if (properEntryAppeared && either.isLeft) Seq()
          else {
            if (either.isRight) properEntryAppeared = true
            Seq(either)
          }
        }
      }))

      errorSource ~> merge
      entriesSource ~> merge

      merge ~> filter

      SourceShape(filter.out)
    }
    Source.fromGraph(graph)
  }

  private def tickedEntriesSource(initialDelay: FiniteDuration, interval: FiniteDuration): Source[Entry, Cancellable] = {
    logger.info(s"Creating new wykop entries ticking source. Initial delay: [$initialDelay]. Interval: [$interval]")
    Source.tick(initialDelay, interval, Tick())
      .map(_ => RestRequest())
      .via(wykopEntriesFetchFlow)
      .mapConcat[MirkoEntry](identity)
      .statefulMapConcat { () =>
        var biggestIdSoFar: Long = 0
        entry =>
          if (entry.id > biggestIdSoFar) {
            biggestIdSoFar = entry.id
            Seq(entry)
          } else {
            Seq.empty
          }
      }
      .map(e => MirkoEntry.convertToEntry(e))
  }

}

object WykopApiHandler {

  case class RestRequest()

  val RedundantInt: Int = 2127

  val InfiniteAttempts: Int = -1

  case class Tick()

  class WykopApiException(val swarmError: SwarmError, private val message: String = "", private val throwable: Throwable) extends Exception(message, throwable) {
    def this(swarmError: SwarmError, message: String) = {
      this(swarmError, message, null)
    }
  }

}
