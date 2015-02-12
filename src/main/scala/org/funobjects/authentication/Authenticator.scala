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

package org.funobjects.authentication

import org.funobjects.authentication.Identified.FutureIdentified

import scala.concurrent.ExecutionContext

/**
 * An Authenticator is a class that can take an identifier of type ID, credentials of type CRED,
 * and a UserRepository that can look up users of type U identified by type ID.
 */
abstract class Authenticator[ID, CRED, U](userRepo: UserRepository[ID, U]) {
   def authenticate(id: ID, cred: CRED)(implicit exec: ExecutionContext): FutureIdentified[ID, U]
}

object Authenticate {
  /**
   * Summon and apply an implicit authenticator, producing an Identified[U] from a U.
   */
  def apply[CRED, ID, U](id: ID, userCred: CRED)
    (implicit auth: Authenticator[ID, CRED, U], exec: ExecutionContext): FutureIdentified[ID, U] = auth.authenticate(id, userCred)
}

class SimpleAuthenticator(userRepo: UserRepository[String, SimpleUser])
  extends Authenticator[String, String, SimpleUser](userRepo) {

  /**
   * [[Identified]] for SimpleUser; user name is the id.
   */
  class SimpleIdentified(u: SimpleUser) extends Identified[String, SimpleUser](u) {
    override def id: String = u.name
  }

  override def authenticate(id: String, cred: String)(implicit exec: ExecutionContext): FutureIdentified[String, SimpleUser] =
    userRepo.findUser(id).map { or => or.map(new SimpleIdentified(_)) }

  class AuthenticatorException(msg: String, cause: Exception = null) extends Exception(msg, cause)
}
