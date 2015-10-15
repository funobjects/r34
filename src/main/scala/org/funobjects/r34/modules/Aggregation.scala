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

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.funobjects.r34.ResourceModule

import scala.concurrent.ExecutionContext

/**
 * Aggregate module container.
 *
 * @author Robert Fries
 */
class Aggregation(id: String, modules: List[ResourceModule])(implicit val sys: ActorSystem, exec: ExecutionContext, flows: ActorMaterializer) extends ResourceModule()(sys, exec, flows) {

  override val name = "root"
  override val routes = Some(aggregateRoutes)
  override val props = Some(Aggregation.props(modules))

  val moduleMap = modules.map(mod => (mod.name, mod)).toMap

  lazy val aggregateRoutes: Route = {

    modules.flatMap {
      // not all modules have routes (route is actually an Option[Route]), but we need the module name
      // for the prefix, so map it into the option so it can be flatMapped over

      mod => mod.routes map { rt => (mod.name, rt) }
    } map {
      // prefix each module route with the module name
      case (nm, route) => pathPrefix(nm) { route }
    } reduceLeft {
      // combine the aggregate routes with ~
      (route1, route2) => route1 ~ route2
    }
  }
}

object Aggregation {

  def props(modules: List[ResourceModule]) = Props(classOf[AggregatingActor], modules)

  class AggregatingActor(modules: List[ResourceModule]) extends Actor {

    var refs: Map[ActorRef, ResourceModule] = Map.empty

    var unhandled = 0

    override def preStart(): Unit = {
      // side-effect some actors into being...

      refs = (
        for (module <- modules;
             props <- module.props)
          yield (context.system.actorOf(props, "module:" + module.name), module)
        ).toMap
    }

    override def receive: Actor.Receive = {
      case Terminated(ref) =>
        refs -= ref
        if (refs.isEmpty) {
          context.stop(self)
        }
      case _ => unhandled += 1
    }
  }
}
