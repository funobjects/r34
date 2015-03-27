/*
 * Copyright 2015 Functional Objects, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.funobjects.r34

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.common.StrictForm
import akka.http.model.headers._
import akka.http.server.Directives._
import akka.http.model._
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.{ConfigFactory, Config}

import org.funobjects.r34.directives.R34Directives.oauth2

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write, writePretty}

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

  def router = {
    logRequestResult("r34") {
      path("shutdown") {
        complete {
          sys.scheduler.scheduleOnce(1.second) { sys.shutdown() }
          HttpResponse(StatusCodes.OK)
        }
      } ~
      (get & path("getuser")) {
        complete {
          HttpResponse(StatusCodes.OK, entity = writePretty(SimpleUser("a", "b")))
        }
      } ~
      (get & path("tauth")) {
        oauth2(tokenRepository) { user =>
          println(s"*** user: $user")
          complete {
            HttpResponse(StatusCodes.OK)
          }
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
        oauth2(tokenRepository) { identifiedUser =>
          complete {
            userRepository.get(userId) map {
              case Good(Some(user)) => HttpResponse(StatusCodes.OK, entity = HttpEntity(write(user)))
              case Good(None)       => HttpResponse(StatusCodes.NotFound)
              case Bad(issues)      => HttpResponse(StatusCodes.BadRequest)
            } recover {
              case NonFatal(ex)     => HttpResponse(StatusCodes.InternalServerError)
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

  override val tokenRepository = new InMemoryRepository[BearerToken, TokenEntry[SimpleUser]]() {
  }

  val serverBinding = Http(sys).bind(interface = "localhost", port = 3434).runForeach { connection =>
    connection.handleWith(router)
  }
}

