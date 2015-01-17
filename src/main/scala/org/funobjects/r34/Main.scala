package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.model.{HttpResponse, HttpRequest}

/**
 * Created by rgf on 1/17/15.
 */
object Main {

  // not using App because of weirdness with field initialization
  def main(args: Array[String]): Unit = {
    import akka.http.Http
    import akka.stream.FlowMaterializer

    implicit val system = ActorSystem()
    implicit val materializer = FlowMaterializer()

    val serverBinding = Http(system).bind(interface = "localhost", port = 8080)
    serverBinding.connections.foreach { connection => // foreach materializes the source
      println("Accepted new connection from " + connection.remoteAddress)
    }
    println("Hello world!")
  }
}
