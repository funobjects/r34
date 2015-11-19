package org.funobjects.r34

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{ConfigFactory, Config}
import org.funobjects.db.orientdb.OrientDbHelper
import org.scalatest.{WordSpec, Matchers, BeforeAndAfterEach}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
 * Created by rgf on 11/18/15.
 */
class R34OneActorSystemPerTest extends WordSpec with BeforeAndAfterEach with Matchers {
  implicit var system: ActorSystem = _
  implicit var exec: ExecutionContext = _
  implicit var mat: ActorMaterializer = _


  // TODO: this should probably be done with withFixture, so we could get rid of the vars
  override protected def beforeEach(): Unit = {
    super.beforeEach()
    system = ActorSystem("ActorSpec", R34OneActorSystemPerSuite.akkaConfig)
    exec = system.dispatcher
    mat = ActorMaterializer()
  }

  override protected def afterEach(): Unit = {
    mat.shutdown()
    Await.result(system.terminate(), 5.second)
    OrientDbHelper.removeDatabase("plocal:testJournal", "admin", "admin")
    super.afterEach()
  }
}

object R34OneActorSystemPerTest {
  val akkaConfig: Config = ConfigFactory.parseString(
    """
      akka.loglevel = WARNING
      akka.log-dead-letters = off
      akka.persistence.journal.plugin = funobjects-akka-orientdb-journal
      funobjects-akka-orientdb-journal.db.url = "plocal:testJournal"
    """)
}
