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

/**
 * Indicates that an entity implements a "soft" delete by marking the entity
 * for deletion.  A timestamp is also provided to allow for later cleanup
 * based on entity age.
 *
 */
trait Deletable[A] {
  def isDeleted(a: A): Boolean
  def delete(a: A): A
  def deletedTime(a: A): Long
}
