package me.kcala.mirkoSwarm.infrastructure

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import me.kcala.mirkoSwarm.infrastructure.Ticker.Tick

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class Ticker(interval: FiniteDuration) {

  def tickSource: Source[Ticker.Tick, Cancellable] = {
    Source.tick(0.seconds, interval, Tick())
  }

}

object Ticker {

  case class Tick()

  def apply(interval: FiniteDuration): Ticker = new Ticker(interval)
}
