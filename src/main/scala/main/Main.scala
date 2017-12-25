package main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Main {

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("MirkoSwarm")
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    val deps: Deps = MirkoSwarmDeps(
      actorSystem = actorSystem,
      actorMaterializer = actorMaterializer,
      executionContext = actorSystem.dispatcher
    )
  }


}
