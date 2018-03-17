package me.kcala.mirkoSwarm.main

import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.config.AppConfig
import me.kcala.mirkoSwarm.deilvery.WebsocketHandler
import me.kcala.mirkoSwarm.wykop.WykopEntriesSource

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  val wykopApiHandler = new WykopEntriesSource(config.wykopApiHost, config.wykopApiKey, config.tickInterval, config.waitOnWykopApiError)
  WebsocketHandler(config.interface, config.port, wykopApiHandler.entriesSource)
  //TODO add `asSink` method to websocketHandler for nicer API
}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}