package wykop

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import main.Deps

import scala.concurrent.Future

class WykopApiHandler()(implicit deps: Deps) {

  import deps._

  def fetchLatestEntries(): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = Uri("http://a.wykop.pl/stream/index/appkey,UbPB8on5Xx")))
  }

}
