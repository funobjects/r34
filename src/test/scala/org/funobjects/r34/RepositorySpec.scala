package org.funobjects.r34

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.testkit._
import org.funobjects.r34.auth._
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

}
