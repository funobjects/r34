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
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.funobjects.r34.auth._
import org.funobjects.r34.modules.TokenModule.TokenEntry
import org.funobjects.r34.{Store, Repository, ResourceModule}
import org.json4s.jackson.Serialization._
import org.scalactic.{Bad, Good}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal

/**
 * Created by rgf on 6/10/15.
 */
class OAuth2Module(implicit sys: ActorSystem,
  executionContext: ExecutionContext,
  mat: ActorMaterializer,
  userRepository: Repository[String, SimpleUser],
  tokenStore: Store[BearerToken, TokenEntry[SimpleUser]]) extends ResourceModule {

  implicit val jsonFormats = org.json4s.DefaultFormats


  override val name: String = "oauth2"
  override val routes: Option[Route] = Some(oauth2routes)

  def oauth2routes = path ("auth" / "token") {
    formFields("grant_type", "scope", "code".?, "username".?, "password".?, "client_id".?, "client_secret".?, "redirect_url".?) {
      (grantType, scope, code, username, password, clientId, clientSecret, redirectUrl) =>
        complete {
          implicit val authenticator = new SimpleAuthenticator()
          TokenRequest(grantType, scope, code, username, password, clientId, clientSecret) match {
            case TokenRequest("password", theScope, _, Some(user), Some(pass), _, _) =>
              Authenticate(user, pass) map {
                case Good(authedUser) =>
                  val tk = BearerToken.generate(32)
                  tokenStore.update(tk, TokenEntry(authedUser.user, Permits.empty, None)) map {
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
