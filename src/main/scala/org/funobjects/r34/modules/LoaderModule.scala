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

import java.net.{URLClassLoader, URL}

import akka.actor._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.funobjects.r34.{Issue, ResourceModule}
import org.scalactic.{Good, Bad, Or, Every}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
 * Module that dynamically loads other modules.
 */
class LoaderModule(implicit val sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends ResourceModule()(sys, exec, mat) {
  override val name: String = LoaderModule.name
  override val props: Option[Props] = Some(Props[LoaderModule.LoaderActor])
  override val routes: Option[Route] = None
}

object LoaderModule {
  val name = "loader"

  sealed trait LoaderRequest

  final case class Load(jar: URL, className: String, sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends LoaderRequest
  final case class Instance(jar: URL, className: String, result: ResourceModule Or Every[Issue])

  case object GetInstances extends LoaderRequest
  final case class Instances(instances: Set[Instance])

  final class LoaderActor extends Actor with ActorLogging {

    var instances: Set[Instance] = Set.empty // TODO: probably needs to be a map

    override def preStart(): Unit = {
      super.preStart()
    }

    override def receive: Receive = {
      case Load(url, className, sys, exec, mat) =>

        try {
          // first, get a class loader for the given jar, then use it to load the module class
          val loader = new URLClassLoader(Array(url), this.getClass.getClassLoader)
          val moduleClass = loader.loadClass(className)

          // make sure it is compatible with the parent class loader's idea of a module
          if (classOf[ResourceModule].isAssignableFrom(moduleClass)) {
            // find the constructor
            val ctor = moduleClass.getConstructor(
              classOf[ActorSystem],
              classOf[ExecutionContext],
              classOf[ActorMaterializer]
            )

            // create the new instance, start it, and then return the instance to the sender
            val newInstance = ctor.newInstance(sys, exec, mat).asInstanceOf[ResourceModule]
            newInstance.start()
            val instance = Instance(url, className, Good(newInstance))
            instances += instance
            sender() ! instance
          } else {
            // not assignment-compatible with with ResourceModule
            sender() ! Instance(url, className, Bad(Issue("Incompatible resource module class: %s", Array(className))))
          }
        } catch {
          case NonFatal(e) => println(e); sender() ! Instance(url, className, Bad(Issue("Failed to load module: ", e)))
        }

      case GetInstances => sender() ! Instances(instances)
      case _ =>
    }
  }
}
