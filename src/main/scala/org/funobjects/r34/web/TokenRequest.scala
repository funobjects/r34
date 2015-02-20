package org.funobjects.r34.web

import java.nio.ByteBuffer
import java.util.{Base64, UUID}

import akka.actor.ActorSystem
import akka.http.model._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import org.funobjects.r34.auth.BearerToken

import scala.concurrent.ExecutionContext
import scala.util.Try


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

  /**
   * spray-json unmarshaller for TokenRequest from form-encoded requests entities.
   */
//   def unmarshaller(implicit sys: ActorSystem, matz: FlowMaterializer, exec: ExecutionContext): Unmarshaller[HttpEntity, TokenRequest] =
//    Unmarshaller[HttpEntity, TokenRequest] { entity =>
//
//      Unmarshal(entity).to[FormData] map { formData =>
//        val grantType     = formData.fields.get("grant_type").getOrElse(throw new IllegalArgumentException("grant_type is required"))
//        val scope         = formData.fields.get("scope").getOrElse(throw new IllegalArgumentException("scope is required"))
//        TokenRequest(grantType, scope,
//          formData.fields.get("code"),
//          formData.fields.get("username"),
//          formData.fields.get("password"),
//          formData.fields.get("client_id"),
//          formData.fields.get("client_secret"))
//      }
//    }

//  implicit def unmarshal(implicit matz: FlowMaterializer, ec: ExecutionContext): FromRequestUnmarshaller[TokenRequest] = Unmarshaller[HttpRequest, TokenRequest] { entity =>
//    Unmarshal(entity).to[FormData] map { formData =>
//      val grantType = formData.fields.get("grant_type").getOrElse(throw new IllegalArgumentException("grant_type is required"))
//      val scope = formData.fields.get("scope").getOrElse(throw new IllegalArgumentException("scope is required"))
//      TokenRequest(grantType, scope,
//        formData.fields.get("code"),
//        formData.fields.get("username"),
//        formData.fields.get("password"),
//        formData.fields.get("client_id"),
//        formData.fields.get("client_secret"))
//    }
//  }
//

  def routes(implicit sys: ActorSystem, flower: FlowMaterializer, executionContext: ExecutionContext) = {
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
