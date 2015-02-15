package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.model._
import HttpMethods._
import StatusCodes._
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.auth.{SimpleUser, SimpleUserRepository}

/**
 * Created by rgf on 1/17/15.
 */
object Main {

  // not using App because of weirdness with field initialization
  def main(args: Array[String]): Unit = {
    import akka.http.Http
    import akka.stream.FlowMaterializer

    val akkaConfig: Config = ConfigFactory.parseString("""
      akka.loglevel = INFO
      akka.log-dead-letters = off""")

    implicit val system = ActorSystem("r34", akkaConfig)
    implicit val materializer = ActorFlowMaterializer()

    implicit val userRepo: SimpleUserRepository = new SimpleUserRepository(Some(Set(
      SimpleUser("userA", "passA"),
      SimpleUser("userB", "passB")
    )))

    val serverBinding = Http(system).bind(interface = "localhost", port = 3434)

    serverBinding.connections.runForeach { connection =>
      println("Accepted new connection from " + connection.remoteAddress)
      connection handleWithSyncHandler localAdmin
    }

    def localAdmin: HttpRequest => HttpResponse = {
      case req @ HttpRequest(POST, Uri.Path("/shutdown"), _, _, _) =>
        system.shutdown()
        HttpResponse(OK)

      case req @ HttpRequest(POST, Uri.Path("/auth/token"), headers, entity, _) =>
        HttpResponse(NotImplemented)

      case req @ HttpRequest(method, uri, headers, entity, protocol) =>
        println(s"req $req")
        HttpResponse(NotFound)

      case _ => HttpResponse(NotFound)
    }
  }
}
