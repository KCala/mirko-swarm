package me.kcala.mirkoSwarm.deilvery

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import me.kcala.mirkoSwarm.main.Deps

import scala.concurrent.Future

class HttpServer(interface: String, port: Int)(implicit deps: Deps) {

  import deps._


}

object HttpServer {
  def apply(interface: String, port: Int)(implicit deps: Deps): HttpServer = new HttpServer(interface, port)
}
