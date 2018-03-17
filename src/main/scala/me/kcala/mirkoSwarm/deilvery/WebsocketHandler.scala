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

class WebsocketHandler(interface: String,
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

  val wykopEntriesBroadcastHub: Source[Either[SwarmError, Entry], NotUsed] = mirkoEntriesSource
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right).run()

  //Consumes stream when there are no subscribers, in order to avoiding clogging
  wykopEntriesBroadcastHub.runForeach(_ => Sink.ignore)

  Http().bindAndHandle(routes, interface, port)
    .foreach { _ =>
      println(s"Handling weboscket connections at ws://$interface:$port/${ApiPrefixSegments.reduce(_ + "/" + _)}/$EntriesEndpoint")
    }

  private def ignoreMessagesAndAttachMirkoEntriesFlow: Flow[Message, Message, Any] = {
    val ignoreMessagesAndAttachMirkoEntries = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val entriesSource: SourceShape[Either[SwarmError, Entry]] = b.add(wykopEntriesBroadcastHub)

      val wsMessages = b.add(Flow[Message])
      val out = b.add(Flow[Message])
      val toJson = Flow[Either[SwarmError, Entry]].map {
        case Right(r) => r.toJson
        case Left(l) => l.toJson
      }
      val toWsMessage = Flow[JsValue].map(e => TextMessage(e.prettyPrint))

      wsMessages ~> Sink.ignore
      entriesSource ~> toJson ~> toWsMessage ~> out

      FlowShape(wsMessages.in, out.out)
    }
    Flow.fromGraph(ignoreMessagesAndAttachMirkoEntries)
  }
}

object WebsocketHandler {
  def apply(interface: String,
            port: Int,
            mirkoEntriesSource: Source[Either[SwarmError, Entry], _])(implicit deps: Deps): WebsocketHandler =
    new WebsocketHandler(interface, port, mirkoEntriesSource)

  val EntriesEndpoint: String = "entries"
  val ApiPrefixSegments: Seq[String] = "api/v1".split('/').toSeq
}
