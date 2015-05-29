package org.funobjects.r34.auth

import akka.dispatch.ExecutionContexts
import org.scalactic.Good
import org.scalatest._
import concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext

/**
 * Created by rgf on 2/14/15.
 */
class UserRepositorySpec extends FlatSpec with Matchers {

  import UserRepositorySpec._

  "SimpleUserRepository" should "return valid users"  in {
    implicit val userRepo = setupRepo()

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
    implicit val userRepo = setupRepo()

    val f = userRepo.get("userC")
    f.futureValue shouldEqual Good(None)
  }
}

object UserRepositorySpec {
  val userA = SimpleUser("userA", "passA")
  val userB = SimpleUser("userB", "passB")
  val userC = SimpleUser("userC", "passC")
  val userD = SimpleUser("userD", "passD")

  def setupRepo() = {
    import Matchers._

    implicit val exec: ExecutionContext = ExecutionContexts.global()

    val repo = new SimpleUserStore()
    val fa = repo.put(userA.name, userA)
    val fb = repo.put(userB.name, userB)
    whenReady (fa) { or => or.isGood shouldBe true }
    whenReady (fb) { or => or.isGood shouldBe true }
    repo
  }

  def setupChainedRepo() = {
    import Matchers._

    implicit val exec: ExecutionContext = ExecutionContexts.global()

    val repo1 = new SimpleUserStore()
    val fa = repo1.put(userA.name, userA)
    val fb = repo1.put(userB.name, userB)
    whenReady (fa) { or => or.isGood shouldBe true }
    whenReady (fb) { or => or.isGood shouldBe true }

    val repo2 = new SimpleUserStore(Some(Set(userC, userD)))
    repo1 orElse repo2
  }
}