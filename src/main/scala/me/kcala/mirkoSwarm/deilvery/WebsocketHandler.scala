package me.kcala.mirkoSwarm.deilvery

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{FlowShape, SourceShape}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.deilvery.WebsocketHandler.{ApiPrefixSegments, EntriesEndpoint}
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.main.Deps
import me.kcala.mirkoSwarm.model.{Entry, SwarmError}
import spray.json.{JsValue, enrichAny}

private class WebsocketHandler(interface: String,
                       port: Int,
                       mirkoEntriesSource: Source[Either[SwarmError, Entry], _])(implicit deps: Deps)
  extends StrictLogging with JsonSupport {

  import deps._

  private val routes: Route =
    concat {
      pathPrefix(ApiPrefixSegments.map(PathMatcher(_)).reduce(_ / _)) {
        path(EntriesEndpoint) {
          get {
            handleWebSocketMessages(ignoreMessagesAndAttachMirkoEntriesFlow)
          }
        }
      }
    }

  Http().bindAndHandle(routes, interface, port)
    .foreach { _ =>
      logger.info(s"Handling weboscket connections at ws://$interface:$port/${ApiPrefixSegments.reduce(_ + "/" + _)}/$EntriesEndpoint")
    }

  private def ignoreMessagesAndAttachMirkoEntriesFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.ignore,
    mirkoEntriesSource.map {
      case Right(r) => r.toJson
      case Left(l) => l.toJson
    })
    .map(json => TextMessage(json.prettyPrint))
}

object WebsocketHandler {
  val EntriesEndpoint: String = "entries"
  val ApiPrefixSegments: Seq[String] = "api/v1".split('/').toSeq

  def sink(interface: String, port: Int)(implicit deps: Deps): Sink[Either[SwarmError, Entry], NotUsed] = {
    BroadcastHub.sink[Either[SwarmError, Entry]](bufferSize = 256).mapMaterializedValue(entriesSource => {
      import deps._
      entriesSource.runForeach(_ => Sink.ignore)
      new WebsocketHandler(interface, port, entriesSource)
      NotUsed
    })
  }
}
