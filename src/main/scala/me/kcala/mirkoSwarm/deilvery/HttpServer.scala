package me.kcala.mirkoSwarm.deilvery

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import me.kcala.mirkoSwarm.main.Deps

import scala.concurrent.Future

class HttpServer(interface: String, port: Int)(implicit deps: Deps) {

  import deps._

  private val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, """ {"hello": "world"} """))
      }
    }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, interface, port)
      .map { binding =>
        println(s"Server online at $interface:$port")
        binding
      }
  }
}

object HttpServer {
  def apply(interface: String, port: Int)(implicit deps: Deps): HttpServer = new HttpServer(interface, port)
}
