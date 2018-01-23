package me.kcala.mirkoSwarm.main

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.config.AppConfig
import me.kcala.mirkoSwarm.deilvery.WebsocketHandler
import me.kcala.mirkoSwarm.infrastructure.Ticker
import me.kcala.mirkoSwarm.wykop.WykopApiHandler

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  val tickerSource: Source[Ticker.Tick, Cancellable] = Ticker(config.tickInterval).tickSource
  val wykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey)

  WebsocketHandler(config.interface, config.port, tickerSource.via(wykopApiHandler.wykopEntriesFlow))
  //TODO add `asSink` method to websocketHandler for nicer API
}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}