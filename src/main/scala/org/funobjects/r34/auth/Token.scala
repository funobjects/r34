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

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.{UUID, Base64}

trait AccessToken
case class BearerToken(token: String) extends AccessToken {
  require(token.length <= BearerToken.maxLen)
  require(BearerToken.valid(token))
}

object BearerToken {
  val maxLen = 1024 * 5
  val regex = """([a-zA-Z0-9[-]_~+/]+=*)""".r

  def valid(t: String): Boolean = t.length < maxLen && regex.pattern.matcher(t).matches

  def generate(size: Int): BearerToken = {
    val bytes = new Array[Byte](size)
    new SecureRandom() nextBytes bytes
    BearerToken(Base64.getUrlEncoder encodeToString bytes)
  }

  def fromUuid(u: UUID): BearerToken = {
    val buf = ByteBuffer allocate (java.lang.Long.BYTES * 2)
    buf putLong u.getMostSignificantBits
    buf putLong u.getLeastSignificantBits
    BearerToken(Base64.getUrlEncoder encodeToString buf.array())
  }
}

case class JwtToken(token: String) {
  require(token.length <= JwtToken.maxLen )
  // TODO: decode and validate JWT Token w/ nimbus
}

object JwtToken extends AccessToken {
  val maxLen = 1024 * 500
}