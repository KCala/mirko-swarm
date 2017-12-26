package me.kcala.mirkoSwarm.main

import akka.NotUsed
import akka.actor.{Actor, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.wykop.Entry

import scala.collection.immutable._
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success, Try}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider

class MirkoSwarmActor(deps: Deps) extends Actor with StrictLogging with JsonSupport {

  import deps._


  val pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = Http().cachedHostConnectionPool[Int]("a.wykop.pl")

  Source.tick(0.seconds, 10.seconds, HttpRequest(uri = Uri("/stream/index/appkey,UbPB8on5Xx")) -> 2)
    .log(logger.underlying.getName)
    .via(pool)
    //      .map(l =>{ println(l); l})
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        println(s"Couldn't fetch entries from Wykop. $ex")
        throw ex
    }
    .mapAsync(10)(resp =>
      Unmarshal(resp.entity).to[Seq[Entry]].recover{
        case thr =>
          println(s"Error deserialising response from wykop. $thr")
          Seq()
      }
    )
    .map(_.reverse)
    .mapConcat[Entry](identity)
        .statefulMapConcat { () =>
          var biggestIdSoFar: Long = 0
          entry =>
            if (entry.id > biggestIdSoFar) {
              biggestIdSoFar = entry.id
              Seq(entry)
            } else {
              Seq.empty
            }
        }
    .map(_.id)
    .toMat(Sink.foreach(e => println(e)))(Keep.right).run()

  override def receive: Receive = {
    case _ => ???
  }
}

object MirkoSwarmActor {
  def props()(implicit deps: Deps) = Props(new MirkoSwarmActor(deps))
}