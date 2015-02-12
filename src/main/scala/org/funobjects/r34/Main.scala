package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.model.{HttpResponse, HttpRequest}
import akka.stream.ActorFlowMaterializer

/**
 * Created by rgf on 1/17/15.
 */
object Main {

  val localAdmin: HttpRequest => HttpResponse = {
    case req @ HttpRequest(method, uri, headers, entity, protocol) =>
      println(s"req $req")
      HttpResponse(204)
  }

  // not using App because of weirdness with field initialization
  def main(args: Array[String]): Unit = {
    import akka.http.Http
    import akka.stream.FlowMaterializer

    implicit val system = ActorSystem()
    implicit val materializer = ActorFlowMaterializer()

    val serverBinding = Http(system).bind(interface = "localhost", port = 3434)
    serverBinding.connections.runForeach { connection => // foreach materializes the source
      println("Accepted new connection from " + connection.remoteAddress)
      connection handleWithSyncHandler localAdmin
    }
    println("Hello world!")
  }
}
