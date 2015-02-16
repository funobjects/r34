package org.funobjects.r34.web

import akka.actor.ActorSystem
import akka.http.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.FlowMaterializer
import org.funobjects.r34.Controller
import org.funobjects.r34.auth.{SimpleBearerTokenRepository, SimpleUserRepository}

import scala.concurrent.ExecutionContext

/**
  * Created by rgf on 2/15/15.
 */
class TokenController(userRepository: SimpleUserRepository, tokenRepository: SimpleBearerTokenRepository)
  (implicit sys: ActorSystem, matz: FlowMaterializer, exec: ExecutionContext) extends Controller {

  def request(httpRequest: HttpRequest): HttpResponse = {
    HttpResponse(StatusCodes.NoContent)
  }
}
