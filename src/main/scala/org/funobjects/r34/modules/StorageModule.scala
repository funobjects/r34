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
import org.funobjects.r34.auth.BearerTokenStore.PutEntry
import org.funobjects.r34.{Store, Repository, Issue, ResourceModule}
import org.scalactic.{Bad, Good, Every, Or}

import scala.collection.SortedSet
import scala.collection.immutable.TreeSet
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

  val masterActorPath = s"/user/$name"
  def entityActorPath(id: String) = s"$masterActorPath/$id"

  val idFilterRegex = s"""([_a-zA-Z][_a-zA-Z0-9-]*)""".r

  type PossibleEntity = Option[ENTITY] Or Every[Issue]
  type FuturePossibleEntity = Future[PossibleEntity]

  // Note: EntityCommands (unlike EntityEvent, ModuleCommand, etc) are path dependent types;
  // this is so that they can have a proper type for the entity itself on the creation side
  // (the message loop on the receive side will still have to do pattern matching on the entity
  // due to type erasure)
  
  sealed trait EntityCommand { val id: String }
  sealed trait EntityCommandResponse

  case class GetEntity(id: String) extends EntityCommand
  case class GetEntityResponse(id: String, result: PossibleEntity) extends EntityCommandResponse

  case class UpdateEntity(id: String, value: ENTITY, create: Boolean = true) extends EntityCommand
  case class CheckAndUpdateEntity(id: String, expectedValue: ENTITY, newValue: ENTITY, create: Boolean = true) extends EntityCommand
  case class UpdateEntityResponse(id: String, result: PossibleEntity) extends EntityCommandResponse

  case class DeleteEntity(id: String) extends EntityCommand
  case class DeleteEntityResponse(key: String, result: PossibleEntity) extends EntityCommandResponse
  
  class StorageMasterActor extends PersistentActor with ActorLogging {

    // runtime state is the map(s) of running actors; we need to map
    var refsByKey: Map[String, ActorRef] = Map.empty

    // persistent state is the index if existing keys (a sorted set)
    var keys: SortedSet[String] = TreeSet.empty

    val userKeyRegex = s""".*/user/store:$resType/([^/]+)""".r

    override def receiveCommand: Receive = {
      case cmd: EntityCommand =>  entityCommand(cmd)

      case event @ IndexEntryAdded(id)    if !(keys contains id)  => persist(event) { keys += _.id }
      case event @ IndexEntryRemoved(id)  if keys contains id     => persist(event) { keys -= _.id }

      case Terminated(ref) => removeRef(ref)

      case ModuleShutdown => refsByKey.values.foreach(_ ! ModuleShutdown)
      case ModuleSnapshot => saveSnapshot(keys)
      case _ =>
    }

    override def preStart(): Unit = {
      println("StorageMaster: " + self.path)
      super.preStart()
    }

    def entityCommand(cmd: EntityCommand): Unit = {
      val idExists = keys.contains(cmd.id)
      val actor = refsByKey.get(cmd.id)
      cmd match {
        case GetEntity(id) => (idExists, actor) match {
          case (true, Some(ref))  => ref forward cmd
          case (true, None)       => newRef(id) forward cmd
          case (false, _)         => sender() ! GetEntityResponse(id, Good(None))
        }

        case UpdateEntity(id, _, create) =>
          (idExists, create, actor) match {
            case (true, _, Some(ref)) => ref forward cmd
            case (true, _, None)      => newRef(id) forward cmd
            case (false, true, _)     => newRef(id) forward cmd
            case (false, false, _)    => UpdateEntityResponse(id, Bad(Issue("Entity does not exist, and create flag was false.")))
          }

        case CheckAndUpdateEntity(id, _, _, create) =>
          (idExists, create, actor) match {
            case (true, _, Some(ref)) => ref forward cmd
            case (true, _, None)      => newRef(id) forward cmd
            case (false, true, _)     => newRef(id) forward cmd
            case (false, false, _)    => UpdateEntityResponse(id, Bad(Issue("Entity does not exist, and create flag was false.")))
          }

        case DeleteEntity(id) => (idExists, actor) match {
          case (true, Some(ref))  => ref forward cmd
          case (true, None)       => newRef(id) forward cmd
          case (false, _)         => sender() ! DeleteEntityResponse(id, Good(None))
        }
      }
    }

    def newRef(key: String): ActorRef = {
      val ref = context.actorOf(entityProps(key), key)
      refsByKey += (key -> ref)
      ref
    }

    def removeRef(ref: ActorRef): Unit = {
      // rather than keep a reverse map (i.e. ref to key), we extract the key from
      // the actor path of the terminated actor
      ref.path.toString match {
        case userKeyRegex(id , _*) => refsByKey -= id
          log.debug(s"*** removed ref for $id")
      }
    }

    override def receiveRecover: Receive = {
      case event @ IndexEntryAdded(id)    if !(keys contains id)  => keys += id
      case event @ IndexEntryRemoved(id)  if keys contains id     => keys -= id
    }

    override def persistenceId: String = resType
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

      case UpdateEntity(id, updated, create) => persist(EntityUpdated(id, entity)) { event =>
        val before = entity
        // note that the entity may be deleted, but this update acts like
        // a put in a map, and unconditionally sets the entity
        entity = foldEvent(event, entity)
        sender ! UpdateEntityResponse(id, Good(Some(entity)))

      }
      case DeleteEntity(id) => persist(EntityUpdated(id, entity)) { event =>
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
              ref.ask(UpdateEntity(key, value))(timeout).mapTo[UpdateEntityResponse] map {
                case UpdateEntityResponse(id, possible) => possible
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
  case class EntityUpdated(id: String, entity: Any) extends EntityEvent
  case class EntityRemoved(id: String) extends EntityEvent

  trait IndexEvent
  case class IndexEntryAdded(id: String) extends IndexEvent
  case class IndexEntryRemoved(id: String) extends IndexEvent

  sealed trait ModuleCommand

  case object ModuleShutdown
  case object ModuleSnapshot
}
