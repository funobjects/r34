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
import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.testkit._
import org.funobjects.r34.auth._
import org.funobjects.r34.modules.StorageModule
import org.funobjects.r34.modules.StorageModule.EntityEvent
import org.funobjects.r34.modules.TokenModule.TokenEntry
import org.scalactic.Good

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 *
 */
class RepositorySpec(sys: ActorSystem) extends TestKit(sys) with WordSpecLike with Matchers with ImplicitSender
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("testActorSystem"))

  override protected def afterAll(): Unit = {
    Await.result(sys.terminate, 5.seconds)
  }

  lazy implicit val exec = sys.dispatcher

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
      implicit val flow = ActorMaterializer()

      val mod = new StorageModule[Int]("mod") {

        override def repository: Option[Repository[String, Int]] = ???

        override def isDeleted(entity: Int)(implicit del: Deletable[Int]): Boolean = entity < 0

        override def delete(entity: Int)(implicit del: Deletable[Int]): Int = -1

        override val name: String = "mod"
      }
      val ref: Option[ActorRef] = mod.props.map(p => sys.actorOf(p))

      ref shouldBe a [Some[_]]
    }
  }

}
