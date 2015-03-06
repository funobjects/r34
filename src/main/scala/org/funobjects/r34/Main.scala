package org.funobjects.r34

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorSystem}
import akka.http.Http
import akka.http.common.StrictForm
import akka.http.model.headers._
import akka.http.server.Directives._
import akka.http.model._
import akka.stream.actor.ActorPublisher
import akka.stream.impl.TickSource
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{FlowMaterializer, ActorFlowMaterializer}
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.auth.BearerTokenRepository.FutureEntry

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import org.funobjects.r34.auth._
import org.scalactic.{Bad, Good}

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Success, Try}
import scala.util.control.NonFatal

case class Outer(a: String, b: Option[String], inner: Inner, maybe: Option[Inner] )
case class Inner(n: Int, m: Option[Int])

trait Server {
  implicit val userRepository: Repository[String, SimpleUser]
  implicit val tokenRepository: Repository[BearerToken, TokenEntry[SimpleUser]]
  implicit val sys: ActorSystem
  implicit val flows: ActorFlowMaterializer
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
        (get & path("user" / Segment)) { userId =>
          // TODO: how to enforce size limits on URL and segment ??
          headerValueByType[Authorization]() { auth =>
            auth.credentials match {
              case OAuth2BearerToken(token) => println(s"OAuth2 Token: $token")
                implicit val formats = Serialization.formats(NoTypeHints)
                complete {
                  tokenRepository.get(BearerToken(token)) flatMap {

                    case Good(Some(tokenEntry)) =>

                      userRepository.get(userId) map {
                        case Good(Some(user)) => HttpResponse(StatusCodes.OK, entity = HttpEntity(write(user)))
                        case Good(None)       => HttpResponse(StatusCodes.NotFound)
                        case Bad(issues)      => HttpResponse(StatusCodes.BadRequest)
                      } recover {
                        case NonFatal(ex)     => HttpResponse(StatusCodes.InternalServerError)
                      }

                    case Good(None)   => Future.successful(HttpResponse(StatusCodes.Unauthorized))
                    case Bad(issues)  => Future.successful(HttpResponse(StatusCodes.BadRequest))
                  } recover {
                    case NonFatal(ex) => HttpResponse(StatusCodes.Unauthorized)
                  }
                }
              case _ => println("Bad Authentication Type")
                complete { HttpResponse(StatusCodes.Unauthorized) }
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
      path("clock") {
        complete {
          HttpResponse(StatusCodes.OK,
            entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`,
              Source(0.seconds, 15.seconds, 0)
                .mapMaterialized(c => ())
                .via(Flow[Int].map(tick => HttpEntity.ChunkStreamPart(s"${new java.util.Date}\n")))
            )
          )
        }
      } ~
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

  override val tokenRepository = new InMemoryRepository[BearerToken, TokenEntry[SimpleUser]]() {}

  val serverBinding = Http(sys).bind(interface = "localhost", port = 3434).runForeach { connection =>
    println(s"connection: $connection")
    connection.handleWith(router)
  }
}

