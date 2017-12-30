package me.kcala.mirkoSwarm.main

import akka.actor.{Actor, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import me.kcala.mirkoSwarm.json.JsonSupport
import me.kcala.mirkoSwarm.model.Entry
import me.kcala.mirkoSwarm.wykop.MirkoEntry

import scala.collection.immutable._
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success, Try}

class MirkoSwarm(deps: Deps) extends StrictLogging with JsonSupport {

  import deps._


  val pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = Http().cachedHostConnectionPool[Int]("a.wykop.pl")

  private val WykopEntriesSource: Source[Entry, Cancellable] = Source.tick(0.seconds, config.tickInterval, HttpRequest(uri = Uri("/stream/index/appkey,UbPB8on5Xx")) -> 2)
    .log(logger.underlying.getName)
    .via(pool)
    .map(_._1)
    .map {
      case Success(rep) => rep
      case Failure(ex) =>
        println(s"Couldn't fetch entries from Wykop. $ex")
        throw ex
    }
    .mapAsync(10)(resp =>
      Unmarshal(resp.entity).to[Seq[MirkoEntry]].recover {
        case thr =>
          println(s"Error deserialising response from wykop. $thr")
          Seq()
      }
    )
    .map(_.reverse)
    .mapConcat[MirkoEntry](identity)
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
    .map(MirkoEntry.convertToEntry)

  WykopEntriesSource
    .toMat(Sink.foreach(e => println(e)))(Keep.right).run()

}

object MirkoSwarm {
  def apply(deps: Deps): MirkoSwarm = new MirkoSwarm(deps)
}