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

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private class HttpServer(interface: String,
                         port: Int,
                         sourceTickInterval: FiniteDuration,
                         mirkoEntriesSource: Source[Entry, _])(implicit deps: Deps)
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

  private def ignoreMessagesAndAttachMirkoEntriesFlow: Flow[Message, Message, Any] =
    Flow.fromSinkAndSource(
      Sink.ignore,
      mirkoEntriesSource.keepAlive(sourceTickInterval + 100.millis, () => {
        logger.warn(s"No entries since $sourceTickInterval. Sending error to the clients.")
        SwarmError("Wykop API not responding")
      }
      ))
      .map {
        case e: Entry => e.toJson
        case err: SwarmError => err.toJson
      }
      .map(json => TextMessage(json.prettyPrint))

}

object HttpServer {
  val EntriesEndpoint: String = "entries"
  val ApiPrefixSegments: Seq[String] = "api/v1".split('/').toSeq

  def sink(interface: String, port: Int, sourceTickInterval: FiniteDuration)(implicit deps: Deps): Sink[Entry, NotUsed] = {
    BroadcastHub.sink[Entry](bufferSize = 256).mapMaterializedValue(entriesSource => {
      import deps._
      entriesSource.runForeach(_ => Sink.ignore)
      new HttpServer(interface, port, sourceTickInterval, entriesSource)
      NotUsed
    })
  }
}
