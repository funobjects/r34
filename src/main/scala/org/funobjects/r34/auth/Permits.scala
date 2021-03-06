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

/**
 * Represents a set of permissions granted to a particular entity.
 */
case class Permits(permits: Set[Permit])

object Permits {
  def apply(permit: Permit): Permits = Permits(Set(permit))
  def empty: Permits = Permits(Set.empty[Permit])
}

case class Permit(scope: String)