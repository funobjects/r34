package org.funobjects.r34.web

import akka.actor.ActorSystem
import akka.http.model.{FormData, HttpEntity}
import akka.http.unmarshalling.{Unmarshal, Unmarshaller}
//import akka.http.marshallers.sprayjson.SprayJsonSupport._

import akka.stream.FlowMaterializer

import scala.concurrent.ExecutionContext

import akka.http.unmarshalling.PredefinedFromEntityUnmarshallers._

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
  def unmarshaller(implicit sys: ActorSystem, matz: FlowMaterializer, exec: ExecutionContext): Unmarshaller[HttpEntity, TokenRequest] =
    Unmarshaller[HttpEntity, TokenRequest] { entity =>

      Unmarshal(entity).to[FormData] map { formData =>
        val grantType     = formData.fields.get("grant_type").getOrElse(throw new IllegalArgumentException("grant_type is required"))
        val scope         = formData.fields.get("scope").getOrElse(throw new IllegalArgumentException("scope is required"))
        TokenRequest(grantType, scope,
          formData.fields.get("code"),
          formData.fields.get("username"),
          formData.fields.get("password"),
          formData.fields.get("client_id"),
          formData.fields.get("client_secret"))
      }
    }
}
