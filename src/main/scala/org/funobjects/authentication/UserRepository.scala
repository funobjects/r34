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

import org.funobjects.Issue
import org.scalactic.{One, Every, Or}
import org.scalactic.OptionSugar._

import scala.concurrent.Future

/**
 * Indicates a service that can provide for the lookup of users of type U
 * based on an identifier of type ID
 */
trait UserRepository[ID, U] {
  def findUser(id: ID): Future[U Or Every[Issue]]
}

/**
 * Simple, immutable, in-memory user repository.
 */
case class SimpleUserRepository[U](map: Map[String, U]) extends UserRepository[String, U] {
  override def findUser(id: String): Future[U Or Every[Issue]] = Future.successful(
    map.get(id).toOr(One(Issue("User %s not found", Array(id))))
  )
}
