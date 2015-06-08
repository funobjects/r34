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

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{FlowMaterializer, ActorFlowMaterializer}
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.r34.auth.SimpleUser
import org.scalactic.{Bad, Good}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Created by rgf on 6/1/15.
 */
class LocalUsersSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  val akkaConfig: Config = ConfigFactory.parseString("""
      akka.loglevel = DEBUG
      akka.log-dead-letters = off
      akka.persistence.journal.plugin = funobjects-akka-orientdb-journal
      prime.id = α
    """)

  implicit val sys = ActorSystem("test", akkaConfig)
  implicit val flows: FlowMaterializer = ActorFlowMaterializer()
  implicit val exec: ExecutionContext = sys.dispatcher

  val tm = Span(5, Seconds)

  "The LocalUsers module" should {
    "support adding entries" in {
      val mod = new LocalUsers()
      val user = SimpleUser("a", "pass")

      mod.start()
      val maybeStore = mod.store
      maybeStore shouldBe a [Some[_]]

      val store = maybeStore.get

      val fput = store.put("a", user)

      //fput.futureValue shouldBe Good(None)


      whenReady(fput, timeout(tm)) { u =>
        u shouldBe Good(None)
//        case Good(Some(user)) =>
//          user shouldBe SimpleUser("a", "ap")
//        case Good(None) =>
//          fail("User not found.")
//        case Bad(issues) =>
//          fail(s"User lookup error: $issues")
      }

      val fget = store.get("a")
      whenReady(fget, timeout(tm)) { u =>
        u shouldBe Good(Some(user))
      }

      val fdel = store.remove("a")
      whenReady(fdel, timeout(tm)) { u =>
        u shouldBe Good(Some(user))
      }

      val fget2 = store.get("a")
      whenReady(fget2, timeout(tm)) { u =>
        u shouldBe Good(None)
      }


    }
  }

  override protected def afterAll(): Unit = {
    sys.shutdown()
    sys.awaitTermination()
  }
}
