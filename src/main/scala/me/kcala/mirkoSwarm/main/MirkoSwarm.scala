package me.kcala.mirkoSwarm.main

import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.config.AppConfig
import me.kcala.mirkoSwarm.infrastructure.Ticker
import me.kcala.mirkoSwarm.model.Entry
import me.kcala.mirkoSwarm.wykop.WykopApiHandler.RestRequest
import me.kcala.mirkoSwarm.wykop.{MirkoEntry, WykopApiHandler}

import scala.collection.immutable._

class MirkoSwarm(config: AppConfig)(implicit deps: Deps) extends StrictLogging {

  import deps._

  val ticker = Ticker(config.tickInterval)
  val wykopApiHandler = new WykopApiHandler(config.wykopApiHost, config.wykopApiKey)

  val WykopEntriesSource: Source[Entry, Cancellable] = ticker.tickSource
    .map(_ => RestRequest())
    .via(wykopApiHandler.mirkoFlow())
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

  WykopEntriesSource
    .toMat(Sink.foreach(e => println(e)))(Keep.right).run()

}

object MirkoSwarm {
  def apply(config: AppConfig)(implicit deps: Deps): MirkoSwarm = new MirkoSwarm(config)
}