package org.funobjects.r34.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorFlowMaterializer
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
  flows: ActorFlowMaterializer,
  executionContext: ExecutionContext,
  userRepository: Repository[String, SimpleUser],
  tokenRepository: Store[BearerToken, TokenEntry[SimpleUser]]) extends ResourceModule {

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
                  tokenRepository.update(tk, TokenEntry(authedUser.user, Permits.empty, None)) map {
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
