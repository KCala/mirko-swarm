package kcala.mirkoSwarm.wykop

import akka.NotUsed
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.json.JsonSupport
import kcala.mirkoSwarm.main.Deps
import kcala.mirkoSwarm.model.{Entry, SwarmError}
import kcala.mirkoSwarm.wykop.WykopApiHandler.{WykopApiException, _}

import scala.collection.immutable.Seq
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}

class WykopApiHandler(wykopApiHost: String,
                      wykopApiKey: String,
                      interval: FiniteDuration,
                      waitOnFail: FiniteDuration)(implicit deps: Deps) extends JsonSupport with StrictLogging {

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

  val entriesSource: Source[Entry, Cancellable] =
    tickedEntriesSource(0.seconds, interval)
      .recoverWithRetries(InfiniteAttempts, {
        case ex: WykopApiException =>
          logger.warn(s"Error connecting to Wykop API. Retrying in $waitOnFail")
          tickedEntriesSource(waitOnFail, interval).mapMaterializedValue(_ => NotUsed)
      })

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
