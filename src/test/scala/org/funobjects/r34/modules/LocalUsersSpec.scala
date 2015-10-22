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

import org.funobjects.r34.{Issue, ActorSpec}
import org.funobjects.r34.auth.SimpleUser
import org.funobjects.r34.modules.StorageModule.ModuleDeleteAll
import org.scalactic.{Every, One, Bad, Good}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.concurrent.ScalaFutures._

/**
 * Tests for the LocalUsers module.
 */
class LocalUsersSpec extends ActorSpec {

  implicit val tm = Span(5, Seconds)
  implicit val patienceConfig = PatienceConfig(tm)

  "The LocalUsers module" should {
    "support adding entries" in {
      val mod = new LocalUsers()
      val user = SimpleUser("a", "pass")

      val ref = mod.start()
      ref shouldBe 'defined

      val maybeStore = mod.store
      maybeStore shouldBe 'defined

      val store = maybeStore.get

      {
        val fget = store.get("a")
        fget.futureValue shouldBe Good(None)
      }

      {
        val fput = store.update("a", user)
        fput.futureValue shouldBe Good(None)
      }

      {
        val fget = store.get("a")
        fget.futureValue shouldBe Good(Some(user))
      }

      {
        val fget = store.delete("a")
        fget.futureValue shouldBe Good(Some(user))
      }

      {
        val fget = store.delete("a")
        fget.futureValue shouldBe a [Bad[_,Issue]]
      }

      {
        val fget = store.get("a")
        fget.futureValue shouldBe Good(None)
      }

      ref.get ! ModuleDeleteAll
    }
  }
}
