package kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

case class MirkoSwarmDeps(
                           actorSystem: ActorSystem,
                           actorMaterializer: ActorMaterializer,
                           executionContext: ExecutionContext
                         ) extends Deps
