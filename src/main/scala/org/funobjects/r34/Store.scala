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

package org.funobjects.r34

import org.scalactic.{Bad, Every, Or}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/**
 * A repository that supports updates and removal of entries.
 */
trait Store[K, V] extends Repository[K, V] {
  def create(key: K, value: V): Future[Option[V] Or Every[Issue]] = update(key, value, create = false)
  
  def update(key: K, value: V, create: Boolean = false): Future[Option[V] Or Every[Issue]]

  def checkAndUpdate(key: K, value: V, expected: V): Future[Option[V] Or Every[Issue]]

  def delete(key: K): Future[Option[V] Or Every[Issue]]

  def updateSync(key: K, value: V, create: Boolean): Option[V] Or Every[Issue] =
    Try(Await.result(update(key, value, create), syncTimeout)) match {
      case Success(result) => result
      case Failure(NonFatal(ex)) => Bad(Issue("Error during update: ", ex))
    }

  def checkAndUpdateSync(key: K, value: V, expected: V): Option[V] Or Every[Issue] =
    Try(Await.result(checkAndUpdate(key, value, expected), syncTimeout)) match {
      case Success(result) => result
      case Failure(NonFatal(ex)) => Bad(Issue("Error during checkAndUpdate: ", ex))
    }

  def deleteSync(key: K): Option[V] Or Every[Issue] =
    Try (Await.result(delete(key), syncTimeout)) match {
      case Success(result) => result
      case Failure(NonFatal(ex)) => Bad(Issue("Error during delete: ", ex))
    }
}
