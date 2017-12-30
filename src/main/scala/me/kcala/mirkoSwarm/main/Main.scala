package me.kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import me.kcala.mirkoSwarm.config.{AppConfig, AppConfigKeys}

object Main extends App {

  implicit val actorSystem: ActorSystem = ActorSystem(AppConfigKeys.MirkoSwarm)
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  val appConfig = AppConfig(actorSystem.settings.config.getConfig(AppConfigKeys.MirkoSwarm))
  implicit val deps: Deps = MirkoSwarmDeps(
    actorSystem = actorSystem,
    actorMaterializer = actorMaterializer,
    executionContext = actorSystem.dispatcher
  )

  MirkoSwarm(appConfig)(deps)
}
