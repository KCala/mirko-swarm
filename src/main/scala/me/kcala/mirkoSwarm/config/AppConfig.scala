package me.kcala.mirkoSwarm.config

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class AppConfig(rawConf: Config) {

  import AppConfigKeys._

  val tickInterval: FiniteDuration = rawConf.getDuration(TickInterval)
  val wykopApiHost: String = rawConf.getString(WykopApiHost)


}

object AppConfig {
  def apply(rawConf: Config): AppConfig = new AppConfig(rawConf)
}

object AppConfigKeys {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val MirkoSwarm = "mirko-swarm"

  val TickInterval = "tick-interval"
  val WykopApiHost = "wykop-api-host"
}
