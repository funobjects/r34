package org.funobjects.r34.web

import akka.actor.ActorSystem
import akka.http.model._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer

import scala.concurrent.ExecutionContext

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

  def routes(implicit sys: ActorSystem, flows: FlowMaterializer, executionContext: ExecutionContext) = {
    path ("token") {
      formFields("grant_type", "scope", "code".?, "username".?, "password".?, "client_id".?, "client_secret".?, "redirect_url".?) {
        (grantType, scope, code, username, password, clientId, clientSecret, redirectUrl) =>
        complete {
          val tr = TokenRequest(grantType, scope, code, username, password, clientId, clientSecret)
          HttpResponse(StatusCodes.OK, entity = s"$tr")
        }
      }
    }
  }
}
