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

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.stream.FlowMaterializer
import org.funobjects.r34.{Store, Repository, Issue, ResourceModule}
import org.scalactic.{Good, Every, Or}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.util.{Try, Failure, Success}

/**
 * Base class for event-sourced entity resource modules.
 */
abstract class StorageModule[ENTITY](resType: String)(implicit val sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {

  import StorageModule._

  /**
   * Defines a funtion to change the state of the entity based
   * on the event.  This is used both for processing events generated
   * from commands and for replay, so it would be best of this was a
   * pure function.  Otherwise, be prepared for side-effects to be repeated
   * the next time the persistent actor behind the entity is restarted.
   * 
   * @param ev The event to fold into the entity.
   * @param entity  The entity into which to fold.
   * @tparam EV The base trait/class for foldable events.
   * @return The entity after folding in the event.
   */
  def foldEvent[EV <: EntityEvent](ev: EV, entity: ENTITY): ENTITY

  def isDeleted(entity: ENTITY): Boolean

  override val name = s"store:$resType"
  
  override val props = Some(Props(classOf[StorageMasterActor], this))

  // TODO: Filter values for resType and id
  val masterActorPath = s"/user/$name"
  def entityActorPath(id: String) = s"$masterActorPath/$id"

  type PossibleEntity = Option[ENTITY] Or Every[Issue]
  type FuturePossibleEntity = Future[PossibleEntity]

  sealed trait EntityCommand { val id: String }
  sealed trait EntityCommandResponse

  case class GetEntity(id: String) extends EntityCommand
  case class GetEntityResponse(key: String, result: PossibleEntity) extends EntityCommandResponse

  case class PutEntity(id: String, value: ENTITY) extends EntityCommand
  case class PutEntityResponse(key: String, result: PossibleEntity) extends EntityCommandResponse

  case class DeleteEntity(id: String) extends EntityCommand
  case class DeleteEntityResponse(key: String, result: PossibleEntity) extends EntityCommandResponse
  
  sealed trait ModuleCommand
  
  case object ModuleShutdown

  class StorageMasterActor extends Actor with ActorLogging {

    var refs: Map[String, ActorRef] = Map.empty
    var keys: Map[ActorRef, String] = Map.empty

    override def preStart(): Unit = println("StorageMaster: " + self.path)

    override def receive: Receive = {
      case req: EntityCommand => refs.getOrElse(req.id, newRef(req.id)).forward(req)
      case Terminated(ref) => removeRef(ref)
      case msg @ ModuleShutdown => refs.values.foreach(_ ! msg)
      case _ =>
    }

    def newRef(key: String): ActorRef = {
      val ref = context.actorOf(entityProps(key), key)
      refs += (key -> ref)
      keys += (ref -> key)
      ref
    }

    def removeRef(ref: ActorRef): Unit = {
      keys.get(ref) match {
        case Some(key) =>
          refs -= key
          keys -= ref
        case _ =>
      }
    }
  }

  class EntityStorageActor(entityId: String, init: ENTITY) extends PersistentActor with ActorLogging {

    var entity: ENTITY = init

    override def persistenceId: String = s"$resType:$entityId"

    override def receiveRecover: Receive = {
      case event: EntityEvent => entity = foldEvent(event, entity)
      case _ =>
    }

    override def receiveCommand: Receive = {
      case GetEntity(id) =>
        sender ! GetEntityResponse(id, Good(if (isDeleted(entity)) None else Some(entity)))

      case PutEntity(id, updated) => persist(EntityUpdated(entity)) { event =>
        val before = entity
        // note that the entity may be deleted, but this update acts like
        // a put in a map, and unconditionally sets the entity
        entity = foldEvent(event, entity)
        sender ! PutEntityResponse(id, Good(Some(entity)))

      }
      case DeleteEntity(id) => persist(EntityUpdated(entity)) { event =>
        val before = entity
        entity = foldEvent(event, entity)
        sender ! DeleteEntityResponse(id, Good(if (isDeleted(before)) None else Some(before)))
      }
      case _ =>
    }
  }

  def entityProps(id: String) = Props(classOf[EntityStorageActor], this, id)

  def repository: Option[Repository[String, ENTITY]] = {
    val timeout = 1.second
    // cascade timeouts so that ordering is more deterministic (given timely scheduling)
    Try(Await.result(sys.actorSelection(masterActorPath).resolveOne(timeout), timeout*2)) match {
      // found the ref
      case Success(ref) => Some(
        new Repository[String, ENTITY] {
          override def get(key: String): FuturePossibleEntity = {
            ref.ask(GetEntity(key))(timeout).mapTo[GetEntityResponse] map {
              case GetEntityResponse(id, possible) => possible
            }
          }
        }
      )
      // TODO: Issue mapping
      case _ => None
    }

  }

  def store: Option[Store[String, ENTITY]] = {
    implicit val timeout = 1.second
    // cascade timeouts so that ordering is more deterministic (given timely scheduling)
    Try(Await.result(sys.actorSelection(masterActorPath).resolveOne(timeout), timeout*2)) match {
      // found the ref
      case Success(ref) => Some(
          new Store[String, ENTITY] {
            override def put(key: String, value: ENTITY): FuturePossibleEntity = {
              ref.ask(PutEntity(key, value))(timeout).mapTo[PutEntityResponse] map {
                case PutEntityResponse(id, possible) => possible
              }
            }

            override def remove(key: String): FuturePossibleEntity ={
              ref.ask(DeleteEntity(key))(timeout).mapTo[DeleteEntityResponse] map {
                case DeleteEntityResponse(id, possible) => possible
              }
            }

            override def get(key: String): FuturePossibleEntity = {
              ref.ask(GetEntity(key))(timeout).mapTo[GetEntityResponse] map {
                case GetEntityResponse(id, possible) => possible
              }
            }
          }
        )
      // TODO: Issue mapping
      case Failure(ex) =>
        println(ex)
        None
    }
  }
}

object StorageModule {
  trait EntityEvent
  case class EntityUpdated(entity: Any) extends EntityEvent
  case class EntityRemoved(id: String) extends EntityEvent
}
