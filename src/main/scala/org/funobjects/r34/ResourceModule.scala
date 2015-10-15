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

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

/**
 * Summary information about a resource module.
 *
 * Modules may define an actor, routes, and/or subscriptions to the event bus.
 *
 * Although modules are immutable, certain methods may have side-effects; for example,
 * the start() method has the side-effect of creating (or attempting to create) an actor
 * in the supplied actor system.
 *
 * @author Robert Fries
 */
//trait ResourceModuleLike {
//  val name: String
//  val props: Option[Props] = None
//  val routes: Option[Route] = None
//  val subscriptions: List[String] = Nil
//}

abstract class ResourceModule(implicit sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) {
  val name: String
  val props: Option[Props] = None
  val routes: Option[Route] = None
  val subscriptions: List[String] = Nil

  /**
   * Try to create the module actor, if defined, in associated actor system.
   * @return
   */
  def start(): Option[ActorRef] = props map { p => sys.actorOf(p, name) }
}
