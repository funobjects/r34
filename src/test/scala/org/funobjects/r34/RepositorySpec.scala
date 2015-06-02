package org.funobjects.r34

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.stream.{FlowMaterializer, ActorFlowMaterializer}
import akka.testkit._
import org.funobjects.r34.auth._
import org.funobjects.r34.modules.StorageModule
import org.funobjects.r34.modules.StorageModule.EntityEvent
import org.scalactic.Good

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}

/**
 *
 */
class RepositorySpec(sys: ActorSystem) extends TestKit(sys) with WordSpecLike with Matchers with ImplicitSender
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("testActorSystem"))

  override protected def afterAll(): Unit = {
    sys.shutdown()
  }

  lazy implicit val exec = sys.dispatcher

  "TokenRepositoryActor" should {
    "do some stuff" in {
      import BearerTokenStore._
      val actor = TestActorRef[BearerTokenStoreActor]

      val token = BearerToken.generate(12)
      val user = SimpleUser("a", "a")
      val permits = Permits(Permit("global"))

      actor ! GetEntry(token)
      val msg = expectMsgClass(classOf[GetEntryResponse])
      msg.value shouldBe Good(None)

      actor ! PutEntry(token, TokenEntry(user, permits, None))
      val putResp = expectMsgClass(classOf[PutEntryResponse])
      putResp.result shouldBe Good(None)

      actor ! GetEntry(token)
      val getResp = expectMsgClass(classOf[GetEntryResponse])
      getResp.value shouldBe Good(Some(TokenEntry(user, permits, None)))

    }
  }

  "StoreModule actor" should {
    "be able to be instantiated" in {
      implicit val flow = ActorFlowMaterializer()

      val mod = new StorageModule[Int]("mod") {

        override def repository: Option[Repository[String, Int]] = ???

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
        override def foldEvent[EV <: EntityEvent](ev: EV, entity: Int): Int = ev match {
          case _ => 42
        }

        override def isDeleted(entity: Int): Boolean = ???

        override val name: String = "mod"
      }
      val ref: Option[ActorRef] = mod.props.map(p => sys.actorOf(p))

      ref shouldBe a [Some[_]]
    }
  }

}
