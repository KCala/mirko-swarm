package kcala.mirkoSwarm.deilvery

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.stream.scaladsl.{BroadcastHub, Flow, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.deilvery.HttpServer.{ApiPrefixSegments, EntriesEndpoint}
import kcala.mirkoSwarm.json.JsonSupport
import kcala.mirkoSwarm.main.Deps
import kcala.mirkoSwarm.model.{Entry, SwarmError}
import spray.json.enrichAny

private class HttpServer(interface: String,
                         port: Int,
                         mirkoEntriesSource: Source[Either[SwarmError, Entry], _])(implicit deps: Deps)
  extends StrictLogging with JsonSupport {

  import deps._

  private val routes: Route =

    pathPrefix(ApiPrefixSegments.map(PathMatcher(_)).reduce(_ / _)) {
      path(EntriesEndpoint) {
        get {
          handleWebSocketMessages(ignoreMessagesAndAttachMirkoEntriesFlow)
        }
      }
    } ~
      pathEndOrSingleSlash {
        getFromResource(s"frontend/index.html")
      } ~
      path(Remaining) { name =>
        getFromResource(s"frontend/$name")
      }


  Http().bindAndHandle(routes, interface, port)
    .foreach { _ =>
      logger.info(s"Handling weboscket connections at ws://$interface:$port/${ApiPrefixSegments.reduce(_ + "/" + _)}/$EntriesEndpoint")
      logger.info(s"Serving frontend at https://$interface:$port/")
    }

  private def ignoreMessagesAndAttachMirkoEntriesFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.ignore,
    mirkoEntriesSource.map {
      case Right(r) => r.toJson
      case Left(l) => l.toJson
    })
    .map(json => TextMessage(json.prettyPrint))
}

object HttpServer {
  val EntriesEndpoint: String = "entries"
  val ApiPrefixSegments: Seq[String] = "api/v1".split('/').toSeq

  def sink(interface: String, port: Int)(implicit deps: Deps): Sink[Either[SwarmError, Entry], NotUsed] = {
    BroadcastHub.sink[Either[SwarmError, Entry]](bufferSize = 256).mapMaterializedValue(entriesSource => {
      import deps._
      entriesSource.runForeach(_ => Sink.ignore)
      new HttpServer(interface, port, entriesSource)
      NotUsed
    })
  }
}
