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
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.persistence.{RecoveryCompleted, PersistentActor}
import akka.stream.ActorMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.{Issue, ResourceModule}
import org.scalactic.{Bad, Good, Or, Every}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Created by rgf on 5/28/15.
 */
class ConfigModule(id: String)(implicit val sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends ResourceModule()(sys, exec, mat) {
  override val name: String = ConfigModule.name
  override val props: Option[Props] = Some(Props(classOf[ConfigModule.ConfigActor], id))
  override val routes: Option[Route] = Some(configRoutes)

  def configRoutes: Route = {
    path("shutdown") {
      complete {
        // TODO: perhaps some notification to the modules would be nice....
        sys.scheduler.scheduleOnce(1.second) { sys.terminate() }
        HttpResponse(StatusCodes.OK)
      }
    } ~
    path(Segment) {
      case "foo" => complete(HttpResponse(StatusCodes.OK, entity = "bar"))
      case "hi" => complete(HttpResponse(StatusCodes.OK, entity = "there"))
      case _ => complete(HttpResponse(StatusCodes.NotFound))
    }
  }

}

object ConfigModule {

  def name = "config"

  def primeProps(id: String) = Props(classOf[ConfigModule.PrimeActor], id)
  def implProps(id: String) = Props(classOf[ConfigModule.ConfigActor], id)

  sealed trait ConfigCmd

  case class SetConfig(cfg: Config) extends ConfigCmd
  case class CheckAndSetConfig(cfg: Config, expected: Config) extends ConfigCmd
  case class MergeConfig(cfg: Config) extends ConfigCmd
  case class CheckAndMergeConfig(cfg: Config, expected: Config) extends ConfigCmd

  case object GetConfig extends ConfigCmd
  case class ConfigResponse(cfg: Config Or Every[Issue]) extends ConfigCmd

  sealed trait ConfigEvent

  case class ConfigUpdated(cfg: Config) extends ConfigEvent
  case class ConfigMerged(cfg: Config) extends ConfigEvent

  /**
   * Main module actor.
   *
   * Because various configuration or filesystem issues can cause the creation
   * of a persistent actor to fail, it is best to avoid using a persistent actor
   * as the main actor of a module. Right now, this actor only creates the persistent
   * actor and relays messages, but this is where more sophisticated fall-back would
   * exist for i/o related configuration failure.
   *
   * @param id
   */
  class PrimeActor(id: String) extends Actor with ActorLogging {

    var cfgActor: Try[ActorRef] = _

    override def preStart(): Unit = {
      super.preStart()
      cfgActor = Try { context.actorOf(implProps(id), "impl") }
      cfgActor.failed.foreach(ex => log.error("Error starting persistent actor: "))
    }

    override def receive: Actor.Receive = {
      case a: Any => cfgActor match {
        case Success(ref) => ref forward a
        case Failure(ex) => log.error("Configuration not available due to error starting persistent actor: " + ex)
      }
    }

    override def postStop(): Unit = {
      cfgActor foreach context.system.stop
      super.postStop()
    }
  }

  class ConfigActor(id: String) extends PersistentActor {

    var cfg: Config = ConfigFactory.empty()

    override def persistenceId: String = "config:" + id

    override def receiveCommand: Receive = receivedCommand
    override def receiveRecover: Receive = processEvent

    def receivedCommand: Receive = {
      case GetConfig                                => sender ! ConfigResponse(Good(cfg))
      case SetConfig(newCfg)                        => setConfig(newCfg)
      case MergeConfig(newCfg)                      => mergeConfig(newCfg)
      case CheckAndSetConfig(newCfg, expectedCfg)   => checkAndSetConfig(newCfg, expectedCfg)
      case CheckAndMergeConfig(newCfg, expectedCfg) => checkAndMerge(newCfg, expectedCfg)
      case cmd: Any                                 => println(s"unknown command: ${cmd.getClass.getName} $cmd")
    }

    def processEvent: Receive = {
      case ConfigUpdated(newCfg)  => cfg = newCfg
      case ConfigMerged(newCfg)   => cfg = newCfg.withFallback(cfg)
      case RecoveryCompleted      => println("recovery complete")
      case ev: AnyRef             => println(s"unknown event: ${ev.getClass.getName} $ev")
    }

    def setConfig(newCfg: Config): Unit =
      persist(ConfigUpdated(newCfg)) { event =>
        val old = cfg
        processEvent(event)
        sender() ! ConfigResponse(Good(old))
      }

    def checkAndSetConfig(newCfg: Config, expectedCfg: Config): Unit =
      if (cfg == expectedCfg) {
        setConfig(newCfg)
      } else {
        sender() ! ConfigResponse(Bad(Issue("CheckAndSetConfig: config does not match expected value.")))
      }


    def mergeConfig(newCfg: Config): Unit =
      persist(ConfigMerged(newCfg)) { event =>
        val old = cfg
        processEvent(event)
        sender() ! ConfigResponse(Good(old))
      }

    def checkAndMerge(newCfg: Config, expectedCfg: Config): Unit =
      if (cfg == expectedCfg) {
        mergeConfig(newCfg)
      } else {
        sender() ! ConfigResponse(Bad(Issue("CheckAndMergeConfig: config does not match expected value.")))
      }

    def logMsg(msg: String): Receive = {
      case a: AnyRef => println(s"$msg: got ${a.getClass.getName}")
    }
  }
}