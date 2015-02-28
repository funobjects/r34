package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Directives._
import akka.http.model._
import akka.stream.{FlowMaterializer, ActorFlowMaterializer}
import com.typesafe.config.{ConfigFactory, Config}

import org.json4s._
import org.json4s.jackson.JsonMethods._

import org.funobjects.r34.auth._

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try
import scala.util.control.NonFatal

case class Outer(a: String, b: Option[String], inner: Inner, maybe: Option[Inner] )
case class Inner(n: Int, m: Option[Int])

trait Server {
  implicit val userRepository: Repository[String, SimpleUser]
  implicit val tokenRepository: Repository[BearerToken, TokenEntry[SimpleUser]]
  implicit val sys: ActorSystem
  implicit val flows: FlowMaterializer
  implicit val exec: ExecutionContext

  // default json4s extractors
  implicit val formats = org.json4s.DefaultFormats

  val router = {
    logRequestResult("r34") {
      path("shutdown") {
        complete {
          sys.scheduler.scheduleOnce(1.second) { sys.shutdown() }
          HttpResponse(StatusCodes.OK)
        }
      } ~
      (post & path("outer") & extract(_.request.entity)) { entity =>
        complete {
          entity.toStrict(1.second) map { strict =>
            Try {
              val outer = parse(strict.data.utf8String).extract[Outer]
              println(outer)
              HttpResponse(StatusCodes.OK)
            } recover {
              case NonFatal(ex) => HttpResponse(StatusCodes.InternalServerError, entity = s"Non-optimal execution: $ex\n")
            }
          }
        }
      } ~
      path("form") {
        formFields('a, "b", "c".?) { (a, b, c) =>
          complete {
            println(s"form: $a $b $c")
            HttpResponse(StatusCodes.OK)
          }
        }
      } ~
      pathPrefix("stream") { web.Streamer.routes } ~
      web.TokenRequest.routes
    }
  }
}

object Main extends App with Server {

  val akkaConfig: Config = ConfigFactory.parseString("""
      akka.loglevel = INFO
      akka.log-dead-letters = off""")

  override implicit val sys = ActorSystem("r34", akkaConfig)
  override implicit val flows = ActorFlowMaterializer()
  override implicit val exec = sys.dispatcher

  override val userRepository: SimpleUserRepository = new SimpleUserRepository(Some(Set(
    SimpleUser("userA", "passA"),
    SimpleUser("userB", "passB")
  )))

  override val tokenRepository = new BearerTokenRepository

  val serverBinding = Http(sys).bind(interface = "localhost", port = 3434).startHandlingWith(router)
  //val streamBinding = Http(sys).bind(interface = "localhost", port = 6868).startHandlingWith(Streamer.flow)
}

