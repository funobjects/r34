package org.funobjects.r34.auth

import org.scalactic.Good
import org.scalatest._
import concurrent.ScalaFutures._

/**
 * Created by rgf on 2/14/15.
 */
class SimpleUserRepositorySpec extends FlatSpec with Matchers {

  import SimpleUserRepositorySpec._

  "SimpleUserRepository" should "return valid users"  in {
    implicit val userRepo = setupRepo

    {
      val f = userRepo.get("userA")
      f.futureValue shouldEqual Good(Some(userA))
    }
    {
      val f = userRepo.get("userB")
      f.futureValue shouldEqual Good(Some(userB))
    }
  }

  "SimpleUserRepository" should "not return invalid users"  in {
    implicit val userRepo = setupRepo

    val f = userRepo.get("userC")
    f.futureValue shouldEqual Good(None)
  }
}

object SimpleUserRepositorySpec {
  val userA = SimpleUser("userA", "passA")
  val userB = SimpleUser("userB", "passB")

  def setupRepo = {
    import Matchers._

    val repo = new SimpleUserRepository()
    val fa = repo.put(userA.name, userA)
    val fb = repo.put(userB.name, userB)
    whenReady (fa) { or => or.isGood shouldBe true }
    whenReady (fb) { or => or.isGood shouldBe true }
    repo
  }
}