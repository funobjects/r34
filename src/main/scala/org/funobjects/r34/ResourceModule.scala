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

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.stream.FlowMaterializer

import scala.concurrent.ExecutionContext

/**
 * Summary information about a resource module.
 *
 * @author Robert Fries
 */
trait ResourceModuleLike {
  val name: String
  val props: Option[Props] = None
  val routes: Option[Route] = None
  val configNames: List[String] = Nil
  val subsriptions: List[String] = Nil
}

abstract class ResourceModule(implicit sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModuleLike
