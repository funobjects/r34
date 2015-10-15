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
import org.funobjects.r34.auth.{Permits, BearerToken}
import org.funobjects.r34.modules.TokenModule.TokenEntry

import scala.concurrent.ExecutionContext

/**
 * Created by rgf on 6/10/15.
 */
class TokenModule[U](implicit sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends StorageModule[TokenEntry[U]]("token")(sys, exec, mat) {

  implicit def entryDeletable = new Deletable[TokenEntry[_]] {
    override def isDeleted(entry: TokenEntry[_]): Boolean = ???

    override def delete(entry: TokenEntry[_]): TokenEntry[_] = ???

    override def deletedTime(entry: TokenEntry[_]): Long = ???
  }


//  /**
//   * Determines if an entity has been deleted.
//   */
//  override def isDeleted(entry: TokenEntry[U]): Boolean = entry.deleted
//
//  /**
//   * Returns a copy of the entity which has been marked for deletion.
//   */
//  override def delete(entity: TokenEntry): TokenEntry[U] = entity.copy(deleted = true)
}

object TokenModule {
  case class TokenEntry[U](user: U, papers: Permits, expires: Option[Long], deleted: Boolean = false)
}
