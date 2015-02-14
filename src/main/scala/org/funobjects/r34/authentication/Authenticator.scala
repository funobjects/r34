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

package org.funobjects.r34.authentication

import org.funobjects.r34.{Issue, Repository}
import org.scalactic._

import scala.concurrent.{Future, ExecutionContext}

/**
 * An Authenticator is a class that can take an identifier of type ID, credentials of type CRED,
 * and a UserRepository that can look up users of type U identified by type ID.
 */
abstract class Authenticator[ID, CRED, U](userRepo: Repository[ID, U]) {
   def authenticate(id: ID, cred: CRED)(implicit exec: ExecutionContext): Future[Identified[U] Or Every[Issue]]
}

object Authenticate {
  /**
   * Summon and apply an implicit authenticator, producing an Identified[U] from a U.
   */
  def apply[CRED, ID, U](id: ID, userCred: CRED)(implicit auth: Authenticator[ID, CRED, U],
                                                 exec: ExecutionContext): Future[Identified[U] Or Every[Issue]] =
    auth.authenticate(id, userCred)
}

class SimpleAuthenticator(userRepo: Repository[String, SimpleUser])
  extends Authenticator[String, String, SimpleUser](userRepo) {

  override def authenticate(id: String, cred: String)(implicit exec: ExecutionContext) =
    userRepo.get(id).map {
      case Good(Some(user)) => Good(Identified(user))
      case Good(None) => Bad(Issue("User %s not found.", id))
      case Bad(issues) => Bad(issues)
    }
}
