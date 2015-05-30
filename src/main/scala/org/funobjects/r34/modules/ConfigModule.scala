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

package org.funobjects.r34.modules

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.persistence.PersistentActor
import akka.stream.FlowMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.ResourceModule

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

/**
 * Created by rgf on 5/28/15.
 */
class ConfigModule(id: String)(implicit val sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {
  override val name: String = "config"
  override val props: Option[Props] = Some(Props(classOf[ConfigModule.ConfigActor], id))
  override val routes: Option[Route] = Some(configRoutes)

  def configRoutes: Route = {
    path("shutdown") {
      complete {
        // TODO: perhaps some notification to the modules would be nice....
        sys.scheduler.scheduleOnce(1.second) { sys.shutdown() }
        HttpResponse(StatusCodes.OK)
      }
    } ~
    path("config" / Segment) {
      case "foo" => complete(HttpResponse(StatusCodes.OK, entity = "bar"))
      case "hi" => complete(HttpResponse(StatusCodes.OK, entity = "there"))
      case _ => complete(HttpResponse(StatusCodes.NotFound))
    }
  }
}

object ConfigModule {

  sealed trait ConfigCmd

  case class SetConfig(cfg: Config) extends ConfigCmd
  case class CheckAndSetConfig(cfg: Config, expected: Config) extends ConfigCmd
  case class MergeConfig(cfg: Config) extends ConfigCmd
  case class CheckAndMergeConfig(cfg: Config, expected: Config) extends ConfigCmd

  case object GetConfig extends ConfigCmd
  case class ConfigResponse(cfg: Config) extends ConfigCmd

  sealed trait ConfigEvent

  case class ConfigUpdated(cfg: Config) extends ConfigEvent
  case class ConfigMerged(cfg: Config) extends ConfigEvent

  class ConfigActor(id: String) extends PersistentActor {

    var cfg: Config = ConfigFactory.empty()

    override def persistenceId: String = "config:" + id

    override def receiveCommand: Receive = logMsg("Command") andThen receivedCommand
    override def receiveRecover: Receive = logMsg("Recover") andThen processEvent

    def receivedCommand: Receive = {
      case GetConfig => sender ! cfg
      case SetConfig(newCfg) => setConfig(newCfg)
      case CheckAndSetConfig(newCfg, expectedCfg) => checkAndSetConfig(newCfg, expectedCfg)
      case MergeConfig(newCfg) => mergeConfig(newCfg)
      case CheckAndMergeConfig(newCfg, expectedCfg) => checkAndMerge(newCfg, expectedCfg)
      case _ =>
    }

    def processEvent: Receive = {
      case ConfigUpdated(newCfg) => cfg = newCfg
      case ConfigMerged(newCfg) => cfg = newCfg.withFallback(cfg)
    }

    def setConfig(newCfg: Config): Unit = persist(ConfigUpdated(newCfg)) { event => processEvent(event) }

    def checkAndSetConfig(newCfg: Config, expectedCfg: Config): Unit = if (cfg == expectedCfg) setConfig(newCfg)

    def mergeConfig(newCfg: Config): Unit = persist(ConfigMerged(newCfg)) { event => processEvent(event) }

    def checkAndMerge(newCfg: Config, expectedCfg: Config): Unit = if (cfg == expectedCfg) mergeConfig(newCfg)

    def logMsg(msg: String): Receive = {
      case a: Any => println("$msg: Received command")
    }
  }

}