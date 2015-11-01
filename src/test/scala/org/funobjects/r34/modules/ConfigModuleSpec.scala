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

import akka.actor.{ActorIdentity, Identify, ActorSystem, ActorRef}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.funobjects.r34.ActorSpec
import org.funobjects.r34.modules.ConfigModule._
import org.scalactic.Good
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration._

/**
 * Created by rgf on 10/9/15.
 */

class ConfigModuleSpec extends ActorSpec {

  implicit val akkaTimeout = Timeout(5.seconds)
  implicit val futureTimeout = PatienceConfig(Span(5, Seconds), Span(1, Seconds))

  "ConfigModule" should {

    val cfg = new ConfigModule("1")

    // module is immutable, but start has a side-effect on the actor system (starts the module actor)
    val ref = cfg.start().value

    "create the module actor at the expected path" in {
      val resp = (system.actorSelection("/user/config") ? Identify(0))
        .mapTo[ActorIdentity]
        .futureValue(futureTimeout)
    }

    "return an empty config before receiving any add or merge messages" in {

      val resp = (ref ? ConfigModule.GetConfig)
        .mapTo[ConfigResponse]
        .futureValue(futureTimeout)

      resp shouldBe ConfigResponse(Good(ConfigFactory.empty()))
    }

    "allow a config to be set" in {
      val cfg = ConfigFactory.parseString(
        """
          |group1.key1 = val1
          |group2.key2 = val2
        """.stripMargin)

      val oldCfg = (ref ? SetConfig(cfg))
        .mapTo[ConfigResponse]
        .futureValue(futureTimeout)

      oldCfg.cfg shouldBe Good(ConfigFactory.empty())

      val newCfg = (ref ? ConfigModule.GetConfig)
        .mapTo[ConfigResponse]
        .futureValue(futureTimeout)

      newCfg.cfg shouldBe Good(cfg)
    }

    "fail to update the config if check semantics are used and the check value does not match" in {
      val newCfg = ConfigFactory.parseString("group3.key3 = val3")

      val checkCfg = ConfigFactory.parseString(
        """
          |group1.key1 = val1
          |group2.key2 = val9
        """.stripMargin)

      val oldCfg = (ref ? CheckAndSetConfig(newCfg, checkCfg))
        .mapTo[ConfigResponse]
        .futureValue(futureTimeout)

    }
  }
}
