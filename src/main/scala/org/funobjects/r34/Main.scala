package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.server.Directives._
import akka.http.model._

import akka.stream.ActorFlowMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.auth.{SimpleBearerTokenRepository, SimpleUser, SimpleUserRepository}
import org.funobjects.r34.web
import org.funobjects.r34.web.TokenRequest

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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
    //val tokenController = new TokenController(userRepo, tokenRepo)

    val serverBinding = Http(system).bind(interface = "localhost", port = 3434)

    val router = {
      logRequestResult("r34") {
        path("foo") {
          complete {
            Future.successful(HttpResponse(StatusCodes.NoContent))
          }
        } ~
          pathPrefix("bar") {
            complete {
              Future.successful(HttpResponse(StatusCodes.NoContent))
            }
          } ~
          path("shutdown") {
            complete {
              system.scheduler.scheduleOnce(1.second) { system.shutdown() }
              HttpResponse(StatusCodes.OK)
            }
          } ~
          path("baz") {
            formFields('a, "b", "c".?) { (a, b, c) =>
              complete {
                println(s"$a $b $c")
                HttpResponse(StatusCodes.OK)
              }
            }
          } ~
        web.TokenRequest.routes
      }
    }

    serverBinding.startHandlingWith(router)
  }
}

