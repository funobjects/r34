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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.{ConfigFactory, Config}

import org.funobjects.r34.modules._
import org.funobjects.r34.auth._

import scala.concurrent.ExecutionContext

trait Server {
  implicit val sys: ActorSystem
  implicit val flows: ActorMaterializer
  implicit val exec: ExecutionContext

  val instanceId = Option(System.getProperty("r34.id")).getOrElse("local")

  val configModule = new ConfigModule(instanceId)
  val debugModule = new DebugResources()
  val adminModule = new LocalAdmin()
  val userModule = new LocalUsers()
  val tokenModule = new TokenModule[SimpleUser]()
  val all = new Aggregation(instanceId, List(configModule, adminModule, userModule, tokenModule, debugModule))

  val noRoute: Route = { reject }
}

object Main extends App with Server {

  // TODO: set up real config
  val akkaConfig: Config = ConfigFactory.parseString("""
      akka.loglevel = INFO
      akka.log-dead-letters = off
      akka.persistence.journal.plugin = funobjects-akka-orientdb-journal
      prime.id = α
      """)

  override implicit val sys = ActorSystem("r34", akkaConfig)
  override implicit val flows = ActorMaterializer()
  override implicit val exec = sys.dispatcher


  all.routes foreach { routes =>
    val serverBinding = Http(sys).bind(interface = "localhost", port = 3434).runForeach { connection =>
      println(s"connect => ${connection.remoteAddress}")
      connection.handleWith(routes)
    }
  }
}

