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

class WykopApiHandler(wykopApiHost: String)(implicit deps: Deps) extends JsonSupport with StrictLogging {

  import deps._

  private val pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](wykopApiHost)

  def mirkoFlow(): Flow[RestRequest, Seq[MirkoEntry], NotUsed] = Flow[RestRequest]
    .map(_ => HttpRequest(uri = Uri("/stream/index/appkey,UbPB8on5Xx")) -> RedundantInt)
    .via(pool)
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        println(s"Couldn't fetch entries from Wykop. $ex")
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

}

object WykopApiHandler {

  case class RestRequest()

  val RedundantInt: Int = 42

  def apply(wykopApiHost: String)(implicit deps: Deps): WykopApiHandler = new WykopApiHandler(wykopApiHost)
}
