package org.funobjects.r34

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import org.funobjects.r34.auth.{Identified, SimpleUserRepository, SimpleUser}
import org.scalactic.{Good, Bad, Or, Every}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

/**
 * Created by rgf on 2/28/15.
 */
class FlowStuffSpec extends WordSpec with Matchers with BeforeAndAfterAll with Eventually {

  implicit val sys: ActorSystem = ActorSystem("sys")
  implicit val exec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val flows: ActorFlowMaterializer = ActorFlowMaterializer()

  "Flows" should {
    "work concurrently" in {

      type idUserOrError = Identified[SimpleUser] Or Every[Issue]

      implicit val userRepository: SimpleUserRepository = new SimpleUserRepository(Some(Set(
        SimpleUser("userA", "passA"),
        SimpleUser("userB", "passB")
      ))) {
        override def get(key: String): Future[Or[Option[SimpleUser], Every[Issue]]] = {
          println(s"*** user get $key")
          super.get(key)
        }
      }

      def flowUserGet(name: String): RunnableFlow[Future[Option[SimpleUser] Or Every[Issue]]]
        = Source(userRepository.get(name)).toMat(Sink.head())(Keep.right)

      def flowUserRemove(name: String): RunnableFlow[Future[Option[SimpleUser] Or Every[Issue]]]
        = Source(userRepository.remove(name)).toMat(Sink.head())(Keep.right)

      def completeUserOption(results: Try[Option[SimpleUser] Or Every[Issue]]) = results match {
        case Success(Good(user)) => println(s"found user $user")
        case Success(Bad(issues)) => println(s"application error $issues")
        case Failure(ex) => println(s"*** future failed $ex")
      }

      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserRemove("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }
      flowUserGet("userA").run() onComplete { res => completeUserOption(res) }

    }
  }

  override protected def afterAll(): Unit = {
    sys.shutdown()
    sys.awaitTermination()
    super.afterAll()
  }
}
