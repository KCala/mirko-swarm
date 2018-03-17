package me.kcala.mirkoSwarm.wykop

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.FanOutShape
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.main.Deps
import me.kcala.mirkoSwarm.model.{Entry, SwarmError}
import me.kcala.mirkoSwarm.wykop.WykopApiHandler.{RedundantInt, RestRequest}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class WykopApiHandler(wykopApiHost: String, wykopApiKey: String)(implicit deps: Deps) extends JsonSupport with StrictLogging {

  import deps._

  private val mirkoEntriesEndpoint = s"/stream/index/appkey,$wykopApiKey"

  private val connectionPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](wykopApiHost)

  /**
    * This flow outputs a sequence of entries from Wykop.pl API on every ping
    * The size of returned sequence is equal to the number of entries returned from API (around 50)
    * It is almost sure that there will be duplicate entries between subsequent API calls
    */
  private val wykopEntriesFetchFlow: Flow[RestRequest, Either[MirkoError, Seq[MirkoEntry]], NotUsed] = Flow[RestRequest]
    .map(_ => HttpRequest(uri = Uri(mirkoEntriesEndpoint)) -> RedundantInt)
    .via(connectionPool)
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        logger.error(s"Couldn't fetch entries from Wykop. $ex")
        throw ex
    }
    .mapAsync(10)(resp => {
      Unmarshal(resp.entity).to[Seq[MirkoEntry]]
        .map(Right(_))
        .recoverWith {
          case _ => {
            Unmarshal(resp.entity).to[MirkoError].map(Left(_))
          }
        }
    }
    )
    .map(_.map(_.reverse))

  val wykopEntriesFlow: Flow[Any, Either[SwarmError, Entry], NotUsed] =
    Flow[Any]
      .map(_ => RestRequest())
      .via(wykopEntriesFetchFlow)
      .mapConcat[Either[MirkoError, MirkoEntry]] {
      case Right(entries) => entries.map(Right(_))
      case Left(error) => Seq(Left(error))
    }
      .statefulMapConcat { () =>
        var biggestIdSoFar: Long = 0
        either =>
          either match {
            case Right(entry) =>
              if (entry.id > biggestIdSoFar) {
                biggestIdSoFar = entry.id
                Seq(Right(entry))
              } else {
                Seq.empty
              }
            case Left(error) =>
              Seq(Left(error))
          }
      }
      .map {
        case Right(entry) => Right(MirkoEntry.convertToEntry(entry))
        case Left(error) => Left(MirkoError.convertToError(error))
      }
    .map{ either =>
      either match {
        case Left(error) => logger.warn(s"Error occured while fetching data from Wykop [${error.error}]")
      }
      either
    }

}

object WykopApiHandler {

  case class RestRequest()

  val RedundantInt: Int = 42
}
