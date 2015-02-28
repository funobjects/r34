package org.funobjects.r34

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, WordSpecLike, BeforeAndAfterAll}

/**
 * Base mix-in for tests working with actors.
 */
trait ActorSpec extends WordSpecLike with BeforeAndAfterAll with Matchers
{
  self: TestKit =>

  override protected def afterAll(): Unit = {
    system.shutdown()
    super.afterAll()
  }
}
