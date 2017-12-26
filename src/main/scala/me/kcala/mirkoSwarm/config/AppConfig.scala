package me.kcala.mirkoSwarm.config

import java.time.Duration

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class AppConfig(rawConf: Config) {

  import AppConfigKeys._

  def tickInterval: FiniteDuration = rawConf.getDuration(TickInterval)


}

object AppConfig {
  def apply(rawConf: Config): AppConfig = new AppConfig(rawConf)
}

object AppConfigKeys {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val MirkoSwarm = "mirko-swarm"
  val TickInterval = "tickInterval"
}
