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

import akka.actor.{Props, ActorLogging, Actor, ActorSystem}
import akka.stream.ActorMaterializer
import org.funobjects.r34.ResourceModule

import scala.concurrent.ExecutionContext

/**
 * Module used for testing the module interface
 */
class TestModule(implicit sys: ActorSystem, exec: ExecutionContext, mat: ActorMaterializer) extends ResourceModule {
  override val name: String = TestModule.modName
  override val props: Option[Props] = Some(Props[TestModule.TestModuleActor])
}

object TestModule {
  val modName = "test"

  class TestModuleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case "ping" => sender() ! "pong"
    }
  }
}
