package me.kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

case class MirkoSwarmDeps(
                           actorSystem: ActorSystem,
                           actorMaterializer: ActorMaterializer,
                           executionContext: ExecutionContext,
                           appConfig: Config
                         ) extends Deps
