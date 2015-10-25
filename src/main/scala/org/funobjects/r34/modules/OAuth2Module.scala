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

package org.funobjects.r34.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import org.funobjects.r34.auth._
import org.funobjects.r34.modules.OAuth2Module.TokenEntry
import org.funobjects.r34.Repository
import org.json4s.jackson.Serialization._
import org.scalactic.{Bad, Good}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal

/**
 * Basic OAuth2 processing
 */
class OAuth2Module
  (implicit sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer, userRepository: Repository[String, SimpleUser])
  extends StorageModule[TokenEntry[SimpleUser]]("oauth2")(sys, exec, mat) {

  implicit val jsonFormats = org.json4s.DefaultFormats

  implicit val authenticator = new SimpleAuthenticator()

  override val routes: Option[Route] = Some(oauth2routes)

  def oauth2routes = path ("token") {
    // TODO: use Issue.asJson for error entitities
    formFields("grant_type", "scope", "code".?, "username".?, "password".?, "client_id".?, "client_secret".?, "redirect_url".?) {
      (grantType, scope, code, username, password, clientId, clientSecret, redirectUrl) =>
        complete {
          store match {
            case None => Future.successful(HttpResponse(StatusCodes.InternalServerError, entity = "auth db unavailable"))

            case Some(rep) =>
              TokenRequest(grantType, scope, code, username, password, clientId, clientSecret) match {
                case TokenRequest("password", theScope, _, Some(user), Some(pass), _, _) =>
                  Authenticate(user, pass) map {
                    case Good(authedUser) =>
                      val tk = BearerToken.generate(32)
                      rep.update(tk.token, TokenEntry(authedUser.user, Permits.empty, None)) map {
                        case Good(prev) => HttpResponse(StatusCodes.OK, entity = writePretty("token" -> tk.token))
                        case _ => HttpResponse(StatusCodes.BadRequest)
                      } recover {
                        case NonFatal(ex) => HttpResponse(StatusCodes.InternalServerError)
                      }
                    case Bad(issues) => Future.successful(HttpResponse(StatusCodes.InternalServerError))
                  }
                case _ => Future.successful(HttpResponse(StatusCodes.BadRequest, entity = "request type not recognized"))
              }
          }
        }
    }
  }
}

object OAuth2Module {
  case class TokenEntry[U](user: U, permits: Permits, expires: Option[Long], deleted: Boolean = false) {
    def expired(now: Long) = expires.exists(_ <= now)
  }
  object Directives {
    def oauth2(implicit tokenRepository: Repository[BearerToken, TokenEntry[SimpleUser]]): Directive1[Identified[SimpleUser]] =
      headerValueByType[Authorization](()) flatMap { auth =>
        auth.credentials match {
          case OAuth2BearerToken(token) => println(s"OAuth2 Token: $token")
            tokenRepository.getSync(BearerToken(token)) match {
              case Good(Some(tokenEntry)) =>
                println(s"*** oauth2 authenticated ${tokenEntry.user}")
                provide(Identified(tokenEntry.user))
              case _ =>
                println(s"*** oauth2 failed - token not found")
                reject(AuthorizationFailedRejection)
            }
          case _ =>
            println(s"*** oauth2 failed - bad auth method")
            reject(AuthorizationFailedRejection)
        }
      }
  }
}