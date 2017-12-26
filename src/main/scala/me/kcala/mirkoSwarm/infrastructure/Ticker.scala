package me.kcala.mirkoSwarm.infrastructure

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import me.kcala.mirkoSwarm.infrastructure.Ticker.Tick

import scala.concurrent.duration.FiniteDuration

class Ticker(interval: FiniteDuration) {

  def tickSource: Source[Ticker.Tick.type, Cancellable] = {
    Source.tick(interval, interval, Tick)
  }

}

object Ticker {
  case class Tick()

  def apply(interval: FiniteDuration): Ticker = new Ticker(interval)
}
