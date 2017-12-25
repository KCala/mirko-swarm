package main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Main extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("MirkoSwarm")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  val deps: Deps = MirkoSwarmDeps(
    actorSystem = actorSystem,
    actorMaterializer = actorMaterializer,
    executionContext = actorSystem.dispatcher,
    appConfig = actorSystem.settings.config.getConfig("mirko-swarm")
  )
}
