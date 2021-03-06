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

import org.funobjects.r34.MemoryStore

import scala.concurrent.ExecutionContext

class SimpleUserStore(init: Option[Set[SimpleUser]] = None)(implicit exec: ExecutionContext) extends MemoryStore[String, SimpleUser]()(exec) {
  for (users <- init; user <- users) map.put(user.name, user)
}
