/*
 *  Copyright 2015 Functional Objects, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.funobjects.r34

import org.scalactic.{Bad, Every, Good, Or}

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * A read-only or structure which maintains a value type V
 * with a key type of K.
 */
trait Repository[K, V] { self =>

  val syncTimeout = 1.second

  def get(key: K): Future[Option[V] Or Every[Issue]]

  def getSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def orElse(nextRepo: Repository[K, V])(implicit exec: ExecutionContext): Repository[K, V] = new Repository[K, V] {

    override def get(key: K): Future[Or[Option[V], Every[Issue]]] = {
      self.get(key) flatMap {
        case Good(None) => nextRepo.get(key)
        case or: Or[_,_] => Future.successful(or)
      }
    }
  }
}


