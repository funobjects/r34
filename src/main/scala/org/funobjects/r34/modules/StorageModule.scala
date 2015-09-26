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
import akka.persistence.{SnapshotSelectionCriteria, SnapshotOffer, PersistentActor}
import akka.stream.FlowMaterializer
import org.funobjects.r34._
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
   * Defines a function to change the state of the entity based
   * on the event.  This is used both for processing events generated
   * from commands and for replay, so it would be best of this was a
   * pure function.  Otherwise, be prepared for side-effects to be repeated
   * the next time the persistent actor behind the entity is restarted.
   * 
   * @param ev      The event to fold.
   * @param entity  The entity into which the event should be folded.
   * @tparam EV     The base trait/class for foldable events.
   * @return        The entity after folding in the event.
   */
  def foldEvent[EV <: EntityEvent](ev: EV, entity: Option[ENTITY]): Option[ENTITY] = ev match {
      case EntityUpdated(id, any) =>
        println(s"*** Fold: setting value of $id to $any")
        any match {
          // TODO: this is effectively a cast, since we can't check against ENTITY due to type erasure
          case ent: ENTITY => Some(ent)
        }
      case EntityRemoved(id) =>
        println(s"*** Fold: removing value for $id")
        None
      case _ => entity
    }

  /**
   * Determines if an entity has been deleted.
   */
  def isDeleted(entity: ENTITY)(implicit del: Deletable[ENTITY]): Boolean = del.isDeleted(entity)

  /**
   * Returns a copy of the entity which has been marked for deletion.
   */
  def delete(entity: ENTITY)(implicit del: Deletable[ENTITY]): ENTITY = del.delete(entity)

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

  case class CreateEntity(id: String, initialValue: ENTITY) extends EntityCommand
  case class CreateEntityResponse(id: String, result: PossibleEntity) extends EntityCommandResponse

  case class UpdateEntity(id: String, value: ENTITY, create: Boolean = true) extends EntityCommand
  case class CheckAndUpdateEntity(id: String, newValue: ENTITY, expectedValue: ENTITY) extends EntityCommand
  case class UpdateEntityResponse(id: String, result: PossibleEntity) extends EntityCommandResponse

  case class DeleteEntity(id: String) extends EntityCommand
  case class DeleteEntityResponse(key: String, result: PossibleEntity) extends EntityCommandResponse

  class StorageMasterActor extends PersistentActor with ActorLogging {

    override def persistenceId: String = resType

    // runtime state is the map(s) of running actors
    var refMap: Map[String, ActorRef] = Map.empty

    // persistent state is the index of existing keys (a sorted set)
    var keys: SortedSet[String] = TreeSet.empty

    // Regex used to extract an id from a StorageEntityActor path
    val userKeyRegex = s""".*/user/store:$resType/([^/]+)""".r


    override def receiveCommand: Receive =
    {
      case cmd: EntityCommand =>
        println(s"*** EntityCommand: $cmd")
        entityCommand(cmd)

      case event @ IndexEntryCreated(id)  if !(keys contains id)  => persist(event) { keys += _.id }
        println(s"*** IndexEntityAdded: $id")

      case event @ IndexEntryRemoved(id)  if keys contains id     => persist(event) { keys -= _.id }
        println(s"*** IndexEntityRemoved: $id")

      case Terminated(ref) => removeRef(ref)
        println(s"*** Terminated: $ref")

      case ModuleShutdown => refMap.values.foreach(_ ! ModuleShutdown)

      case ModuleSnapshot => saveSnapshot(keys)

      case ModuleDeleteAll =>
        for (key <- keys;
             ref <- refMap get key) {
          ref ! ModuleDeletePermanent(key)
        }

      case a: Any => println(s"**** Received unexpected msg: ${a.getClass.getName}")
    }

    def entityCommand(cmd: EntityCommand): Unit = {
      println("*** Received entity command...")
      val idExists = keys.contains(cmd.id)
      cmd match {

        case GetEntity(id) =>
          println(s"*** GetEntity $id")
          idExists match {
            case true  => refMap getOrElse(id, newRef(id)) forward cmd
            case false => sender() ! GetEntityResponse(id, Good(None))
          }

        case CreateEntity(id, entity) => idExists match {
            case true  => sender ! CreateEntityResponse(id, Bad(Issue("Entity exists.")))
            case false => persistIndexEvent(IndexEntryCreated(id), id, cmd)
          }
          println(s"*** CreateEntity $id")

        case UpdateEntity(id, _, create) => idExists match {
            case true  => refMap getOrElse(id, newRef(id)) forward cmd
            case false if !create => sender ! UpdateEntityResponse(id, Bad(Issue("Entity does not exist, and create flag was false.")))
            case false if create  => persistIndexEvent(IndexEntryCreated(id), id, cmd)
          }
          println(s"*** UpdateEntity $id")

        case CheckAndUpdateEntity(id, _, _) => idExists match {
            case true  => refMap getOrElse(id, newRef(id)) forward cmd
            case false => sender ! UpdateEntityResponse(id, Bad(Issue("Entity does not exist.")))
          }
          println(s"*** CheckAndUpdateEntity $id")

        case DeleteEntity(id) => idExists match {
            case true  => persistIndexEvent(IndexEntryRemoved(id), id, cmd)
            case false => sender ! DeleteEntityResponse(id, Good(None))
          }
          println(s"*** DeleteEntity $id")
      }
    }

    def persistIndexEvent(ev: IndexEvent, id: String, cmd: EntityCommand): Unit =
      persist(ev) { ev =>
        indexEvent(ev)
        refMap getOrElse(id, newRef(id)) forward cmd
      }

    def newRef(key: String): ActorRef = {
      println(s"*** creating entity actor for: $key")
      val ref = context.actorOf(entityProps(key), key)
      refMap += (key -> ref)
      ref
    }

    def removeRef(ref: ActorRef): Unit = {
      // rather than keep a reverse map (i.e. ref to key), we extract the key from
      // the actor path of the terminated actor
      ref.path.toString match {
        case userKeyRegex(id , _*) => refMap -= id
          println(s"*** removed ref for $id")
      }
    }

    override def receiveRecover: Receive = {
      case event: IndexEvent => indexEvent(event)
    }

    def indexEvent(event: IndexEvent) = event match {
      case IndexEntryCreated(id) if !(keys contains id)  => keys += id
      case IndexEntryRemoved(id) if keys contains id     => keys -= id
    }
  }

  class EntityStorageActor(entityId: String) extends PersistentActor with ActorLogging {

    var entity: Option[ENTITY] = None

    override def persistenceId: String = s"$resType:$entityId"

    override def receiveRecover: Receive = {
      case event: EntityEvent => entity = foldEvent(event, entity)
      case _ =>
    }

    override def receiveCommand: Receive = {

      case GetEntity(id) => sender ! GetEntityResponse(id, Good(entity))

      case CreateEntity(id, updated) => persistAndFold(id, EntityUpdated(id, updated))

      case UpdateEntity(id, updated, _) => persistAndFold(id, EntityUpdated(id, updated))

      case CheckAndUpdateEntity(id, updated, expected) =>
        if (entity.contains(expected))
          persistAndFold(id, EntityUpdated(id, entity))
        else
          sender ! UpdateEntityResponse(id, Bad(Issue("Value check failed: current value does not match expected value.")))

      case DeleteEntity(id) => persist(EntityRemoved(id)) { event =>
        val before = entity
        entity = foldEvent(event, entity)
        sender ! DeleteEntityResponse(id, Good(before))
      }

      case SnapshotOffer(_, snapshot) => snapshot match { case ent: ENTITY => entity = Some(ent) }

      case ModuleDeletePermanent(id) =>
        deleteMessages(Long.MaxValue, permanent = true)
        deleteSnapshots(SnapshotSelectionCriteria(Long.MaxValue, Long.MaxValue))

      case _ => println("*** Received unexpected msg...")
    }

    def persistAndFold(id: String, event: EntityEvent): Unit = {
      println(s"Persisting event for $id: $event")
      persist(event) { ev =>
        val before = entity
        // note that the entity may be deleted, but this update acts like
        // a put in a map, and unconditionally sets the entity
        entity = foldEvent(event, entity)
        sender ! UpdateEntityResponse(id, Good(before))
      }
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
    implicit val timeout = 5.second
    // cascade timeouts so that ordering is more deterministic (given timely scheduling)
    Try(Await.result(sys.actorSelection(masterActorPath).resolveOne(timeout), timeout*2)) match {
      // found the ref
      case Success(ref) => Some(
        new Store[String, ENTITY] {
            override def update(key: String, value: ENTITY, create: Boolean): FuturePossibleEntity = {
              println("** Store: update")
              ref.ask(UpdateEntity(key, value))(timeout).mapTo[UpdateEntityResponse] map {
                case UpdateEntityResponse(id, possible) => possible
              }
            }

            override def checkAndUpdate(key: String, value: ENTITY, expected: ENTITY): FuturePossibleEntity = {
              println("** Store: checkAndUpdate")
              ref.ask(CheckAndUpdateEntity(key, value, expected))(timeout).mapTo[UpdateEntityResponse] map {
                case UpdateEntityResponse(id, possible) => possible
              }
            }

            override def delete(key: String): FuturePossibleEntity ={
              println("** Store: delete")
              ref.ask(DeleteEntity(key))(timeout).mapTo[DeleteEntityResponse] map {
                case DeleteEntityResponse(id, possible) => possible
              }
            }

            override def get(key: String): FuturePossibleEntity = {
              println("** Store: get, sending GetEntity")
              ref.ask(GetEntity(key))(timeout).mapTo[GetEntityResponse] map {
                case GetEntityResponse(id, possible) => possible
              }
            }
          }
        )
      // TODO: Issue mapping
      case Failure(ex) =>
        println("Failed to find ref for master...")
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
  case class IndexEntryCreated(id: String) extends IndexEvent
  case class IndexEntryRemoved(id: String) extends IndexEvent

  sealed trait ModuleCommand

  case object ModuleShutdown
  case object ModuleSnapshot
  case object ModuleDeleteAll
  case class ModuleDeletePermanent(id: String)
}
