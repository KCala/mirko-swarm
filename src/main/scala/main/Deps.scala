package main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

/**
  * Container for dependencies used across the system.
  * It should be kept as small as possible.
  */
trait Deps {
  implicit val actorSystem: ActorSystem
  implicit val actorMaterializer: ActorMaterializer
  implicit val executionContext: ExecutionContext
}
