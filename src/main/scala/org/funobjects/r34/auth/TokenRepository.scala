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

package org.funobjects.r34.auth

import akka.actor.{Props, Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.funobjects.r34.{Repository, Issue}
import org.scalactic.{Good, Every, Or}

import scala.concurrent.Future
import scala.concurrent.duration._

case class TokenEntry[U](user: U, papers: Permits, expires: Option[Long])

/**
 * A simple in-memory token repository based on a concurrent Map.
 */
class BearerTokenRepository(implicit sys: ActorSystem) extends Repository[BearerToken, TokenEntry[SimpleUser]] {

  import BearerTokenRepository._

  val actor = sys.actorOf(Props[BearerTokenRepositoryActor])
  implicit val exec = sys.dispatcher
  implicit val timeout = Timeout(1.second)

  override def getEntry(key: BearerToken): FutureEntry = {
    (actor ? GetEntry(key)).mapTo[GetEntryResponse] map { resp =>
      resp.value
    }
  }

  override def putEntry(key: BearerToken, value: TokenEntry[SimpleUser]): FutureEntry = {
    (actor ? PutEntry(key, value)).mapTo[PutEntryResponse] map { resp =>
      resp.result
    }
  }

  override def removeEntry(key: BearerToken): FutureEntry = {
    (actor ? RemoveEntry(key)).mapTo[RemoveEntryResponse] map { resp =>
      resp.result
    }
  }
}

object BearerTokenRepository {

  type Entry = TokenEntry[SimpleUser]
  type PossibleEntry = Option[Entry] Or Every[Issue]
  type FutureEntry = Future[PossibleEntry]

  sealed trait Req
  sealed trait Resp

  case class GetEntry(key: BearerToken) extends Req
  case class GetEntryResponse(key: BearerToken, value: PossibleEntry) extends Resp

  case class PutEntry(key: BearerToken, v: Entry) extends Req
  case class PutEntryResponse(key: BearerToken, result: PossibleEntry) extends Resp

  case class RemoveEntry(key: BearerToken) extends Req
  case class RemoveEntryResponse(key: BearerToken, result: PossibleEntry) extends Resp

  case class RemoveEntriesForUser(key: BearerToken) extends Req
  case class RemoveEntriesForUserResponse(key: BearerToken, result: PossibleEntry) extends Resp
}

class BearerTokenRepositoryActor extends Actor {

  import BearerTokenRepository._

  val map = collection.mutable.Map[BearerToken, TokenEntry[SimpleUser]]()

  override def receive: Receive = {
    case GetEntry(key) => sender ! GetEntryResponse(key, Good(map.get(key)))
    case PutEntry(key, value) => sender ! PutEntryResponse(key, Good(map.put(key, value)))
    case RemoveEntry(key) => sender ! PutEntryResponse(key, Good(map.remove(key)))
  }
}