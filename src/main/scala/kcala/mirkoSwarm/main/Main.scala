package kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.config.{AppConfig, AppConfigKeys}

object Main extends App with StrictLogging {

  implicit val actorSystem: ActorSystem = ActorSystem(AppConfigKeys.MirkoSwarm)

//  val decider: Supervision.Decider = {
//    case e: WykopApiException =>
//      logger.warn("Problem connecting to Wykop API. Dropping this tick", e)
//      Supervision.Resume
//    case other =>
//      logger.error("Unhandled exception in stream", other)
//      Supervision.Restart
//  }

  implicit val actorMaterializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem)
//            .withSupervisionStrategy(decider)
    )

  val appConfig = AppConfig(actorSystem.settings.config.getConfig(AppConfigKeys.MirkoSwarm))

  implicit val deps: Deps = MirkoSwarmDeps(
    actorSystem = actorSystem,
    actorMaterializer = actorMaterializer,
    executionContext = actorSystem.dispatcher
  )

  MirkoSwarm(appConfig)(deps)
}
