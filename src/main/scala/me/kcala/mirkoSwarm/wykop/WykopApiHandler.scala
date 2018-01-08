package me.kcala.mirkoSwarm.wykop

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.main.Deps
import me.kcala.mirkoSwarm.wykop.WykopApiHandler.{RedundantInt, RestRequest}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

class WykopApiHandler(wykopApiHost: String, wykopApiKey: String)(implicit deps: Deps) extends JsonSupport with StrictLogging {

  import deps._

  /**
    * This flow outputs a sequence of entries from Wykop.pl API on every ping
    * The size of returned sequence is equal to the number of entries returned from API (around 50)
    * It is almost sure that there will be duplicate entries between subsequent API calls
    */
  lazy val wykopEntriesFetchFlow: Flow[RestRequest, Seq[MirkoEntry], NotUsed] = Flow[RestRequest]
    .map(_ => HttpRequest(uri = Uri(mirkoEntriesEndpoint)) -> RedundantInt)
    .via(connectionPool)
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        logger.error(s"Couldn't fetch entries from Wykop. $ex")
        throw ex
    }
    .mapAsync(10)(resp =>
      Unmarshal(resp.entity).to[Seq[MirkoEntry]].recover {
        case thr =>
          println(s"Error deserialising response from wykop. $thr")
          Seq()
      }
    )
    .map(_.reverse)


  private val connectionPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](wykopApiHost)

  private lazy val mirkoEntriesEndpoint = s"/stream/index/appkey,$wykopApiKey"

}

object WykopApiHandler {

  case class RestRequest()

  val RedundantInt: Int = 42
}
