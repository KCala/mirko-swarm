package kcala.mirkoSwarm.main

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.config.AppConfig
import kcala.mirkoSwarm.deilvery.HttpServer
import kcala.mirkoSwarm.model.Entry
import kcala.mirkoSwarm.wykop.WykopApiHandler

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  import deps._

  val wykopApiHandler: WykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey, config.tickInterval, config.waitOnWykopApiError)

  val entriesSource: Source[Entry, Cancellable] = wykopApiHandler.entriesSource
  val websocketSink: Sink[Entry, NotUsed] = HttpServer.sink(config.interface, config.port, config.tickInterval)

  entriesSource.to(websocketSink).run()
}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}