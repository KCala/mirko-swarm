package me.kcala.mirkoSwarm.wykop

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.main.Deps

import scala.concurrent.Future

class WykopApiHandler()(implicit deps: Deps) extends JsonSupport with StrictLogging {

  import deps._

  def fetchLatestEntries(): Future[List[MirkoEntry]] = {
    Http()
      .singleRequest(HttpRequest(
        uri = Uri("http://a.wykop.pl/stream/index/appkey,UbPB8on5Xx")))
      .flatMap(resp =>
        Unmarshal(resp.entity).to[List[MirkoEntry]]
      )
  }

}

object WykopApiHandler {

  case class FetchLatestEntries()

}
