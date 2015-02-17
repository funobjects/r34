package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.model._
import HttpMethods._
import akka.http.unmarshalling.{Unmarshaller, FromEntityUnmarshaller, Unmarshal}

import akka.stream.ActorFlowMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.auth.{SimpleBearerTokenRepository, SimpleUser, SimpleUserRepository}
import org.funobjects.r34.web.TokenController

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

/**
 * Created by rgf on 1/17/15.
 */
object Main {

  // not using App because of weirdness with field initialization
  def main(args: Array[String]): Unit = {
    import akka.http.Http

    val akkaConfig: Config = ConfigFactory.parseString("""
      akka.loglevel = INFO
      akka.log-dead-letters = off""")

    implicit val system = ActorSystem("r34", akkaConfig)
    implicit val materializer = ActorFlowMaterializer()

    val userRepo: SimpleUserRepository = new SimpleUserRepository(Some(Set(
      SimpleUser("userA", "passA"),
      SimpleUser("userB", "passB")
    )))

    val tokenRepo = new SimpleBearerTokenRepository

    val tokenController = new TokenController(userRepo, tokenRepo)

    val serverBinding = Http(system).bind(interface = "localhost", port = 3434)

    serverBinding startHandlingWithSyncHandler localAdmin

    def localAdmin: HttpRequest => HttpResponse = {
      case req @ HttpRequest(POST, Uri.Path("/shutdown"), _, _, _) =>
        system.shutdown()
        HttpResponse(StatusCodes.OK)

      case req @ HttpRequest(POST, Uri.Path("/auth/token"), headers, entity, _) =>

        HttpResponse(StatusCodes.NotImplemented)

      case req @ HttpRequest(method, uri, headers, entity, protocol) =>
        println(s"req $req")
        HttpResponse(StatusCodes.NotFound)

      case _ => HttpResponse(StatusCodes.NotFound)
    }
  }
}
