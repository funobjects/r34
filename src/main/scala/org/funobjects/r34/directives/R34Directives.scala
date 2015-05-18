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

package org.funobjects.r34.directives


import akka.http.scaladsl.model.headers.{OAuth2BearerToken, Authorization}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

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
