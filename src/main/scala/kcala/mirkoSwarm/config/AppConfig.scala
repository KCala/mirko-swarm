package kcala.mirkoSwarm.config

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class AppConfig(rawConf: Config) {

  import AppConfigKeys._

  val tickInterval: FiniteDuration = rawConf.getDuration(TickInterval)
  val waitOnWykopApiError: FiniteDuration = rawConf.getDuration(WaitOnWykopApiError)
  val wykopApiHost: String = rawConf.getString(WykopApiHost)
  val wykopApiKey: String = rawConf.getString(WykopApiKey)
  val interface: String = rawConf.getString(Interface)
  val port: Int = rawConf.getInt(Port)


}

object AppConfig {
  def apply(rawConf: Config): AppConfig = new AppConfig(rawConf)
}

object AppConfigKeys {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val MirkoSwarm = "mirko-swarm"

  val TickInterval = "tick-interval"
  val WaitOnWykopApiError = "wait-on-wykop-api-error"
  val WykopApiHost = "wykop-api-host"
  val WykopApiKey = "wykop-api-key"
  val Interface = "interface"
  val Port = "port"
}
