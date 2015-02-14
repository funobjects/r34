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

package org.funobjects.r34.authentication

trait AccessToken
case class BearerToken(token: String) {
  require(token.length <= BearerToken.maxLen)
  require(BearerToken.valid(token))
}

object BearerToken {
  val maxLen = 1024 * 5
  val regex = """([a-zA-Z0-9[-]_~+/]+=*)""".r
  def valid(t: String): Boolean = t.length < maxLen && regex.pattern.matcher(t).matches
}

case class JwtToken(token: String) {
  require(token.length <= JwtToken.maxLen )
  // TODO: decode and validate JWT Token w/ nimbus
}

object JwtToken {
  val maxLen = 1024 * 500
}