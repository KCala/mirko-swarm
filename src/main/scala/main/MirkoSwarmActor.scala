package main

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.unmarshalling.Unmarshal
import wykop.WykopApiHandler

import scala.util.{Failure, Success}

class MirkoSwarmActor(deps: Deps) extends Actor with ActorLogging {

  import deps._

  val handler = new WykopApiHandler()(deps)
  handler.fetchLatestEntries().flatMap(resp => Unmarshal(resp.entity).to[String])
    .onComplete {
      case Success(respText) =>
        log.info(respText)
        context.stop(self)
      case Failure(e) =>
        log.warning("Couldn't fetch entries from Wykop", e)
        context.stop(self)
    }

  override def receive: Receive = {
    case _ => ???
  }
}

object MirkoSwarmActor {
  def props()(implicit deps: Deps) = Props(new MirkoSwarmActor(deps))
}