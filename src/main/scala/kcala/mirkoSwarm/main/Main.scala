package kcala.mirkoSwarm.main

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshaller.EitherUnmarshallingException
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.scalalogging.StrictLogging
import kcala.mirkoSwarm.config.{AppConfig, AppConfigKeys}

object Main extends App with StrictLogging {

  implicit val actorSystem: ActorSystem = ActorSystem(AppConfigKeys.MirkoSwarm)

  val decider: Supervision.Decider = {
    case e: EitherUnmarshallingException =>
      logger.error("Unhandled exception in stream", e)
      Supervision.Restart
  }

  implicit val actorMaterializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem)
            .withSupervisionStrategy(decider)
    )

  val appConfig = AppConfig(actorSystem.settings.config.getConfig(AppConfigKeys.MirkoSwarm))

  implicit val deps: Deps = MirkoSwarmDeps(
    actorSystem = actorSystem,
    actorMaterializer = actorMaterializer,
    executionContext = actorSystem.dispatcher
  )

  MirkoSwarm(appConfig)(deps)
}
