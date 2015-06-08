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

        override def isDeleted(entity: Int): Boolean = entity < 0

        override def deleted(entity: Int): Int = -1

        override val name: String = "mod"
      }
      val ref: Option[ActorRef] = mod.props.map(p => sys.actorOf(p))

      ref shouldBe a [Some[_]]
    }
  }

}
