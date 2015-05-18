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

package org.funobjects.r34.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import org.funobjects.r34.Repository
import org.funobjects.r34.auth.Authenticate._
import org.funobjects.r34.auth._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write, writePretty}
import org.scalactic.{Bad, Good}


import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal

/**
 * Represents an OAuth2 Token Request.
 */
case class TokenRequest(
   grantType: String,
   scope: String,
   code: Option[String],
   username: Option[String],
   password: Option[String],
   client_id: Option[String],
   client_secret: Option[String])

object TokenRequest {

  def routes(implicit sys: ActorSystem,
    flows: ActorFlowMaterializer,
    executionContext: ExecutionContext,
    userRepository: Repository[String, SimpleUser],
    tokenRepository: Repository[BearerToken, TokenEntry[SimpleUser]]) = {

    implicit val formats = org.json4s.DefaultFormats

    path ("auth" / "token") {
      formFields("grant_type", "scope", "code".?, "username".?, "password".?, "client_id".?, "client_secret".?, "redirect_url".?) {
        (grantType, scope, code, username, password, clientId, clientSecret, redirectUrl) =>
          complete {
            implicit val authenticator = new SimpleAuthenticator()
            TokenRequest(grantType, scope, code, username, password, clientId, clientSecret) match {
              case TokenRequest("password", theScope, _, Some(user), Some(pass), _, _) =>
                Authenticate(user, pass) map {
                  case Good(authedUser) =>
                    val tk = BearerToken.generate(32)
                    tokenRepository.put(tk, TokenEntry(authedUser.user, Permits.empty, None)) map {
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
