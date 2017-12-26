package me.kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import me.kcala.mirkoSwarm.config.AppConfig

import scala.concurrent.ExecutionContext

/**
  * Container for dependencies used across the system.
  * It should be kept as small as possible.
  */
trait Deps {
  implicit val actorSystem: ActorSystem
  implicit val actorMaterializer: ActorMaterializer
  implicit val executionContext: ExecutionContext
  val config: AppConfig
}
