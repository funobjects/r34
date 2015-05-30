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

import akka.actor.{Props, Actor, ActorLogging, ActorSystem}
import akka.persistence.PersistentActor
import akka.stream.FlowMaterializer
import org.funobjects.r34.ResourceModule

import scala.concurrent.ExecutionContext

/**
 * Created by rgf on 5/29/15.
 */
case class LocalUsers(implicit sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {
  override val name: String = "userdb"
  override val routes = None
  override val props = Some(Props[LocalUsers.UserActor])
}

object LocalUsers {



  class UserManager extends Actor with ActorLogging {
    override def receive: Receive = {
      case _ =>
    }
  }

  class UserActor(uid: String) extends PersistentActor {

    override def persistenceId: String = "uid:" + uid

    override def receiveRecover: Receive = {
      case _ =>
    }

    override def receiveCommand: Receive = {
      case _ =>
    }
  }
}
