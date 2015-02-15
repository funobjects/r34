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

package org.funobjects.r34.auth

import org.funobjects.r34.Issue
import org.scalactic.{One, Every, Bad, Good}
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rgf on 2/14/15.
 */
class AuthenticatorSpec extends FlatSpec with Matchers {

  "Authenticator" should "return an Identified[SimpleUser] for a valid user"  in {
    implicit val userRepo = SimpleUserRepositorySpec.setupRepo
    implicit val simpleAuth = new SimpleAuthenticator()

    val auth = Authenticate("userA", "passA")
    auth.futureValue shouldBe Good(Identified(SimpleUserRepositorySpec.userA))
  }

  "Authenticator" should "return an Issue for an invalid user"  in {
    implicit val userRepo = SimpleUserRepositorySpec.setupRepo
    implicit val simpleAuth = new SimpleAuthenticator()

    val auth = Authenticate("userC", "passA")
    whenReady (auth) {
      case Good(Identified(_)) => fail("The user should not authenticate.")
      case Bad(issues: Every[Issue]) =>
    }
  }

  "Authenticator" should "return an Issue for an invalid password"  in {
    implicit val userRepo = SimpleUserRepositorySpec.setupRepo
    implicit val simpleAuth = new SimpleAuthenticator()

    val auth = Authenticate("userA", "passB")
    whenReady (auth) {
      case Good(Identified(_)) => fail("The user should not authenticate.")
      case Bad(issues: Every[Issue]) =>
    }
  }

}
