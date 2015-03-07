package org.funobjects.r34.directives


import akka.http.model.headers.{OAuth2BearerToken, Authorization}
import akka.http.server.Directives._
import akka.http.server._

import org.funobjects.r34.Repository

import org.funobjects.r34.auth._
import org.scalactic.Good

/**
 * Created by rgf on 3/6/15.
 */
object R34Directives {
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
