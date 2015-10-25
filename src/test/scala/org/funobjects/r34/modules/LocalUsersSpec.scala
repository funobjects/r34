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
import org.scalactic.{Bad, Good}
import org.scalatest.OptionValues._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.concurrent.ScalaFutures._

/**
 * Tests for the LocalUsers module.
 */
class LocalUsersSpec extends ActorSpec {

  implicit val tm = Span(5, Seconds)
  implicit val patienceConfig = PatienceConfig(tm)

  val mod = new LocalUsers()
  val user = SimpleUser("a", "pass")
  val ref = mod.start().value
  val store = mod.store.value

  "The LocalUsers module" should {

    "should not find an entry that hasn't been added" in {
      val fget = store.get("a")
      fget.futureValue shouldBe Good(None)
    }

    "should return Good(None) when adding an entry that did not previously exist" in {
      val fput = store.update("a", user)
      fput.futureValue shouldBe Good(None)
    }

    "should return an entry that has been added" in {
      val fget = store.get("a")
      fget.futureValue shouldBe Good(Some(user))
    }

    "should allow an entry to be deleted" in {
      val fget = store.delete("a")
      fget.futureValue shouldBe Good(Some(user))
    }

    "should return an error when deleting an entry that has been deleted" in {
      val fget = store.delete("a")
      fget.futureValue shouldBe a[Bad[_, Issue]]
    }

    "should return an error when deleting an entry that has not been added" in {
      val fget = store.delete("abc")
      fget.futureValue shouldBe a[Bad[_, Issue]]
    }

    "should return None for an entry that does not exist" in {
      val fget = store.get("a")
      fget.futureValue shouldBe Good(None)
    }

      //ref.get ! ModuleDeleteAll
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }
}
