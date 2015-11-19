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
import akka.stream.ActorMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.db.orientdb.OrientDbHelper
import org.scalatest.{Matchers, WordSpecLike, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Base mix-in for tests working with actors.
 */
class R34OneActorSystemPerSuite extends WordSpecLike with BeforeAndAfterAll with Matchers
{
  implicit val system = ActorSystem("ActorSpec", R34OneActorSystemPerSuite.akkaConfig)
  implicit val exec = system.dispatcher
  implicit val mat = ActorMaterializer()


  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 5.second)
    OrientDbHelper.removeDatabase("plocal:testJournal", "admin", "admin")
    super.afterAll()
  }
}

object R34OneActorSystemPerSuite {
  val akkaConfig: Config = ConfigFactory.parseString(
    """
      akka.loglevel = WARNING
      akka.log-dead-letters = off
      akka.persistence.journal.plugin = funobjects-akka-orientdb-journal
      funobjects-akka-orientdb-journal.db.url = "plocal:testJournal"
    """)
}
