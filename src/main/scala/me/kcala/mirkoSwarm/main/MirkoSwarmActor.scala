package me.kcala.mirkoSwarm.main

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.wykop.WykopApiHandler

import scala.util.{Failure, Success}

class MirkoSwarmActor(deps: Deps) extends Actor with StrictLogging {

  import deps._

  val handler = new WykopApiHandler()(deps)
  handler.fetchLatestEntries()
    .onComplete {
      case Success(respText) =>
        logger.info(respText.toString)
        context.stop(self)
      case Failure(e) =>
        logger.warn("Couldn't fetch entries from Wykop", e)
        context.stop(self)
    }

  override def receive: Receive = {
    case _ => ???
  }
}

object MirkoSwarmActor {
  def props()(implicit deps: Deps) = Props(new MirkoSwarmActor(deps))
}