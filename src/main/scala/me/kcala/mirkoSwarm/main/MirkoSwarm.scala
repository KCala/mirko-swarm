package me.kcala.mirkoSwarm.main

import akka.NotUsed
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives.{complete, concat, get, handleWebSocketMessages, path}
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.config.AppConfig
import me.kcala.mirkoSwarm.infrastructure.Ticker
import me.kcala.mirkoSwarm.model.Entry
import me.kcala.mirkoSwarm.wykop.WykopApiHandler.RestRequest
import me.kcala.mirkoSwarm.wykop.{MirkoEntry, WykopApiHandler}
import me.kcala.mirkoSwarm.deilvery.HttpServer

import scala.collection.immutable._

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  import deps._

  val ticker = Ticker(config.tickInterval)
  val wykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey)

  val WykopEntriesSource: Source[Entry, Cancellable] = ticker.tickSource
    .map(_ => RestRequest())
    .via(wykopApiHandler.wykopEntriesFetchFlow)
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
    .map(MirkoEntry.convertToEntry)

  val wykopEntriesBroadcastHub: Source[Entry, NotUsed] = WykopEntriesSource
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right).run()

//  wykopEntriesBroadcastHub.runForeach(_ => Sink.ignore)


  //    .onComplete { _ =>
  //      logger.info("Mirko stream stopped. Terminating application.")
  //      Http().shutdownAllConnectionPools().flatMap(_ => actorSystem.terminate())
  //  }


  private val route =
    concat {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`application/json`, """ {"hello": "world"} """))
        }
      }
      path("ws") {
        get {
          handleWebSocketMessages(greeter)
        }
      }
    }

  def greeter: Flow[Message, Message, Any] = {
    val ignoreMessagesAndAttachMirkoEntries = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val entriesSubscriber: SourceShape[Entry] = b.add(wykopEntriesBroadcastHub)

      val ws = b.add(Flow[Message])
      val out = b.add(Flow[Message])

      val entryToMessageFlow = Flow[Entry].map(e => TextMessage(e.toString))

      ws ~> Sink.ignore
      entriesSubscriber ~> entryToMessageFlow ~> out


      FlowShape(ws.in, out.out)
    }
    Flow.fromGraph(ignoreMessagesAndAttachMirkoEntries)
  }

  private val interface: String = config.interface
  private val port: Int = config.port
  Http().bindAndHandle(route, interface, port)
    .map { binding =>
      println(s"Server online at $interface:$port")
      binding
    }


}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}