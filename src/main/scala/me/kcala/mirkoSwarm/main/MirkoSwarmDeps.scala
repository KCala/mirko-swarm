package me.kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import me.kcala.mirkoSwarm.config.AppConfig

import scala.concurrent.ExecutionContext

case class MirkoSwarmDeps(
                           actorSystem: ActorSystem,
                           actorMaterializer: ActorMaterializer,
                           executionContext: ExecutionContext
                         ) extends Deps
