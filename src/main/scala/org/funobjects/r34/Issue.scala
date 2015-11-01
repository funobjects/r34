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

package org.funobjects.r34

import org.scalactic.One

import scala.language.implicitConversions

/**
 * Represents an error or other exception while processing.
 */
case class Issue (
  msg: String,
  parms: Array[AnyRef],
  ex: Option[Throwable] = None,
  tag: Option[String] = None) {

  override def toString = String.format(msg, parms: _*) + ex.map("\n" + _.getMessage).getOrElse("")
}

object Issue {
  def apply(msg: String): Issue = new Issue(msg, Array(), None, None)
  def apply(msg: String, ex: Throwable): Issue = new Issue(msg, Array(), Some(ex), None)
  def apply[T <: AnyRef](msg: String, parms: Array[T]): Issue = new Issue(msg, parms.asInstanceOf[Array[AnyRef]], None, None)
  implicit def issueToOneIssue(issue: Issue): One[Issue] = One(issue)
}
