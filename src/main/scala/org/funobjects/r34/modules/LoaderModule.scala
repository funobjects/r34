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

import java.io._
import java.net.{URLClassLoader, URL}

import akka.actor._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.funobjects.r34.{Issue, ResourceModule}
import org.scalactic.{Good, Bad, Or}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
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

  /** Loads a single, named class from the given jar.  Returns an Instance message. */
  final case class LoadOne(jar: URL, className: String, sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends LoaderRequest
  final case class LoadOneResponse(jar: URL, className: String, result: ResourceModule Or Issue)

  /** Load all classes from a jar based on the module list in META-INF/r34modules. Returns an Instance message. */
  final case class LoadAll(jar: URL, sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends LoaderRequest
  final case class LoadAllResponse(jar: URL, result: Seq[ResourceModule] Or Issue)

  case object GetInstances extends LoaderRequest
  final case class Instances(instances: Set[Instance])

  case class Instance(jar: URL, className: String, module: ResourceModule) {
    // start the module (causes side-effects in the actor system, which is an implicit parameter)
    private[modules] def start() = { module.start(); this }
  }

  final class LoaderActor extends Actor with ActorLogging {

    var instances: Set[Instance] = Set.empty // TODO: probably needs to be a map

    override def preStart(): Unit = {
      super.preStart()
    }

    override def receive: Receive = {

      case LoadOne(url, className, sys, exec, mat) =>

        // loading the class loader or instance may turn out Bad, hence the flatMap
        classLoaderFor(url)
          .flatMap { classLoader => loadInstance(classLoader, className, sys, exec, mat) }
          .map { mod =>
            // instance was loaded, so execute the side-effects (starting the module,
            // noting for future reference, and letting the sender know)
            mod.start()
            instances += Instance(url, className, mod)
            sender() ! LoadOneResponse(url, className, Good(mod))
          }
          .recover {
            issue => sender() ! LoadOneResponse(url, className, Bad(issue))
          }

      case LoadAll(url, sys, exec, mat) =>

        classLoaderFor(url)
          .flatMap { classLoader => loadModules(classLoader, sys, exec, mat) }
          .map { mods: Seq[ResourceModule] =>
            // all modules loaded, so do the side-effects (would be foreach except that recover is needed)
            mods.foreach { mod =>
              mod.start()
              instances += Instance(url, mod.getClass.getName, mod)
            }
            sender() ! LoadAllResponse(url, Good(mods))
          }
          .recover {
            // modules were not loaded, forward the reason
            issue => sender() ! LoadAllResponse(url, Bad(issue))
          }

      case GetInstances => sender() ! Instances(instances)
      case _ =>
    }
  }

  def classLoaderFor(url: URL): ClassLoader Or Issue =
    try {
      Good(new URLClassLoader(Array(url)))
    } catch {
      case NonFatal(ex) => Bad(Issue(s"Unable to get class loader for: $url", ex))
    }

  def loadModules(classLoader: ClassLoader, sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer): Seq[ResourceModule] Or Issue = {
    val modFile = "META-INF/r34modules"
    println("loading: " + modFile)
    try {
      Option(classLoader.getResourceAsStream(modFile))
        .map { in =>
          val reader = new BufferedReader(new InputStreamReader(in))
          // for each line, trim comments and whitespace, and then try to load the given class name
          val names: Seq[String] = reader.lines().iterator().asScala.toList.map(_.replaceFirst("#.*", "").trim)
          val seqOrs = names.map(name => loadInstance(classLoader, name, sys, exec, mat))
          // if all modules could be loaded, then return all, otherwise return an issue
          if (seqOrs.forall(_.isGood))
            Good(seqOrs.flatMap(_.toSeq))
          else {
            // get all bads, flatmap into actual issues
            val issues = seqOrs.flatMap {
              case Bad(issue) => Some(issue)
              case Good(mod) => None
            }
            Bad(Issue("Not all modules could be loaded:") ++ issues)
          }
        }
        .getOrElse {
          Bad(Issue("Unable to open module list.") )
        }
    } catch {
      case NonFatal(ex) => Bad(Issue("Unable to open module list.", ex))
    }
  }

  def loadInstance(
    classLoader: ClassLoader,
    name: String,
    sys: ActorSystem,
    exec: ExecutionContext,
    mat: ActorMaterializer): ResourceModule Or Issue = {

    try {
      val moduleClass = classLoader.loadClass(name)
      // make sure it is compatible with the parent class loader's idea of a module
      if (classOf[ResourceModule].isAssignableFrom(moduleClass)) {
        // find the constructor
        Option(moduleClass.getConstructor(
          classOf[ActorSystem],
          classOf[ExecutionContext],
          classOf[ActorMaterializer]))
        .map { ctor =>
          // create the instance
          val newInstance = ctor.newInstance(sys, exec, mat).asInstanceOf[ResourceModule]
          Good(newInstance)
        }
        .getOrElse {
          Bad(Issue("No suitable constructor."))
        }
      } else {
        Bad(Issue("Incompatible module class: " + name))
      }
    } catch {
      case NonFatal(ex) => Bad(Issue("Error loading instance: ", ex))
    }
  }
}
