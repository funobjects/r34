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
import akka.stream.scaladsl._
import org.funobjects.r34.auth.{Identified, SimpleUserStore, SimpleUser}
import org.scalactic.{Good, Bad, Or, Every}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Try, Failure, Success}

/**
 * Created by rgf on 2/28/15.
 */
class FlowStuffSpec extends WordSpec with Matchers with BeforeAndAfterAll with Eventually {

  implicit val sys: ActorSystem = ActorSystem("sys")
  implicit val exec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val mat = ActorMaterializer()

  "Flows" should {
    "work concurrently" in {

      type idUserOrError = Identified[SimpleUser] Or Every[Issue]

      implicit val userRepository: SimpleUserStore = new SimpleUserStore(Some(Set(
        SimpleUser("userA", "passA"),
        SimpleUser("userB", "passB")
      ))) {
        override def get(key: String): Future[Or[Option[SimpleUser], Every[Issue]]] = {
          println(s"*** user get $key")
          super.get(key)
        }
      }

      def flowUserGet(name: String): RunnableGraph[Future[Option[SimpleUser] Or Every[Issue]]]
        = Source(userRepository.get(name)).toMat(Sink.head)(Keep.right)

      def flowUserRemove(name: String): RunnableGraph[Future[Option[SimpleUser] Or Every[Issue]]]
        = Source(userRepository.delete(name)).toMat(Sink.head)(Keep.right)

      def completeUserOption(results: Try[Option[SimpleUser] Or Every[Issue]]) = results match {
        case Success(Good(user)) => println(s"found user $user")
        case Success(Bad(issues)) => println(s"application error $issues")
        case Failure(ex) => println(s"*** future failed $ex")
      }

      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserRemove("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }

    }
  }

  override protected def afterAll(): Unit = {
    Await.result(sys.terminate(), 5.seconds)
    super.afterAll()
  }
}
