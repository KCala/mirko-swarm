package me.kcala.mirkoSwarm.main

import akka.NotUsed
import akka.actor.{Actor, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.wykop.{Entry, WykopApiHandler}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success, Try}

class MirkoSwarmActor(deps: Deps) extends Actor with StrictLogging with JsonSupport {

  import deps._

  val pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), NotUsed] = Http().superPool[Int]()

  Source.tick(0.seconds, 10.seconds, HttpRequest(uri = Uri("http://a.wykop.pl/stream/index/appkey,UbPB8on5Xx")) -> 2)
    .via(pool)
    .map(_._1)
    .collect { case Success(resp) => resp }
    .mapAsync(10)(resp => Unmarshal(resp.entity).to[immutable.Seq[Entry]])
    .mapConcat[Entry](identity)
    .map(e =>
      s"""==================================================================================
         |$e
         |==================================================================================""".stripMargin)
    .toMat(Sink.foreach(e => println(e)))(Keep.right).run()


  //  val handler = new WykopApiHandler()(deps)
  //  handler.fetchLatestEntries()
  //    .onComplete {
  //      case Success(respText) =>
  //        logger.info(respText.toString)
  //        context.stop(self)
  //      case Failure(e) =>
  //        logger.warn("Couldn't fetch entries from Wykop", e)
  //        context.stop(self)
  //    }

  override def receive: Receive = {
    case _ => ???
  }
}

object MirkoSwarmActor {
  def props()(implicit deps: Deps) = Props(new MirkoSwarmActor(deps))
}