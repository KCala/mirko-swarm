package me.kcala.mirkoSwarm.main

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.config.AppConfig
import me.kcala.mirkoSwarm.deilvery.HttpServer
import me.kcala.mirkoSwarm.model.{Entry, SwarmError}
import me.kcala.mirkoSwarm.wykop.WykopApiHandler

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  import deps._

  val wykopApiHandler: WykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey, config.tickInterval, config.waitOnWykopApiError)

  val entriesSource: Source[Either[SwarmError, Entry], Cancellable] = wykopApiHandler.entriesSource
  val websocketSink: Sink[Either[SwarmError, Entry], NotUsed] = HttpServer.sink(config.interface, config.port)

  entriesSource.to(websocketSink).run()
}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}