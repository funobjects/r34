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

package org.funobjects.r34.modules

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.funobjects.r34.Deletable
import org.funobjects.r34.auth.SimpleUser

import scala.concurrent.ExecutionContext

/**
 * Local user DB
 */
class LocalUsers()(implicit sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer)
  extends StorageModule[SimpleUser]("user")(sys, exec, mat) {

  override val routes = None

  override def isDeleted(entity: SimpleUser)(implicit del: Deletable[SimpleUser]): Boolean = entity.isDeleted

  /**
   * Returns a copy of the entity which has been marked for deletion.
   */
  override def delete(entity: SimpleUser)(implicit del: Deletable[SimpleUser]): SimpleUser = entity.copy(isDeleted = true)
}
