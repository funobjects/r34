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

import scala.language.implicitConversions

/**
 * Indicates that the wrapped user type U has been
 * identified and authenticated.  This trait is designed to allow the
 * authentication state of an arbitrary user type to be embedded in the
 * type, allowing code to be written that will only compile when handling
 * an authenticated user. See [[Authenticator]]
 */
case class Identified[U](user: U) {

  /**
   * Execute some code with access to the contained user type.
   */
  def foreach(f: U => Unit): Unit = f(user)

  /**
   * Implicit conversion back to the user type, so that Identified[U] can be
   * used anywhere that U can be used.
   */

  implicit def identifiedToUser(identified: Identified[U]): U = identified.user
}
