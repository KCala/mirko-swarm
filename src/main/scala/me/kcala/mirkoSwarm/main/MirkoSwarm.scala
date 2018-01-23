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
import me.kcala.mirkoSwarm.deilvery.WebsocketHandler

import scala.collection.immutable._

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  import deps._

  val ticker = Ticker(config.tickInterval)
  val wykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey)

  val wykopEntriesSource: Source[Entry, Cancellable] = ticker.tickSource
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

  WebsocketHandler(config.interface, config.port, wykopEntriesSource)

}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}