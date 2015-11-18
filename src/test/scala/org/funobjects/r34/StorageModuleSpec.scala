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

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorRef}
import org.funobjects.r34.StorageModuleSpec.MinimalActor
import org.funobjects.r34.modules.StorageModule

/**
 *
 */
class StorageModuleSpec extends R34TestKitSpec {

//  "TokenRepositoryActor" should {
//    "do some stuff" in {
//      val actor = TestActorRef[BearerTokenStoreActor]
//
//      val token = BearerToken.generate(12)
//      val user = SimpleUser("a", "a")
//      val permits = Permits(Permit("global"))
//
//      actor ! GetEntry(token)
//      val msg = expectMsgClass(classOf[GetEntryResponse])
//      msg.value shouldBe Good(None)
//
//      actor ! PutEntry(token, TokenEntry(user, permits, None))
//      val putResp = expectMsgClass(classOf[PutEntryResponse])
//      putResp.result shouldBe Good(None)
//
//      actor ! GetEntry(token)
//      val getResp = expectMsgClass(classOf[GetEntryResponse])
//      getResp.value shouldBe Good(Some(xTokenEntry(user, permits, None)))
//
//    }
//  }

  "StoreModule actor" should {
    "be able to be instantiated" in {

      val mod = new StorageModule[Int]("mod") {
        override val props = Some(Props[MinimalActor])
      }
      val ref: Option[ActorRef] = mod.start()
      ref shouldBe a [Some[_]]
    }
  }
}

object StorageModuleSpec {
  class MinimalActor extends Actor {
      override def receive: Receive = { case _ => }
  }
}
