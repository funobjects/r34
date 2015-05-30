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

import org.scalactic.{Good, Every, Or}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by rgf on 5/27/15.
 */
class MemoryStore[K, V](implicit val exec: ExecutionContext) extends Store[K, V] {
  val map = collection.concurrent.TrieMap[K, V]()

  override def get(key: K): Future[Option[V] Or Every[Issue]] = {
    Future.successful(Good(map.get(key)))
  }

  override def put(key: K, value: V): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.put(key, value)))

  override def remove(key: K): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.remove(key)))
}
