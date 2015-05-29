package org.funobjects.r34.modules

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.persistence.PersistentActor
import akka.stream.FlowMaterializer
import com.typesafe.config.Config
import org.funobjects.r34.ResourceModule

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * The prime resource of the system.
 *
 * @author Robert Fries
 */
class ConfigModule(id: String)(implicit val sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {

  override val name = "prime"

  override val props: Option[Props] = Some(Props(classOf[PrimeResourceActor], id))
  override val routes: Option[Route] = Some(primeRoutes)

  def primeRoutes: Route = {
    logRequestResult("r34") {
      path("shutdown") {
        complete {
          // TODO: perhaps some notification to the modules would be nice....
          sys.scheduler.scheduleOnce(1.second) { sys.shutdown() }
          HttpResponse(StatusCodes.OK)
        }
      }
      path("r") {
        // TODO: resource module paths go here
        complete(HttpResponse(StatusCodes.OK))
      }
    }
  }
}

object ConfigModule {

  sealed trait ConfigCmd

  case class Shutdown(reason: String = "None")  extends ConfigCmd
  case class NewConfig(cfg: Config) extends ConfigCmd
  case class CheckAndSetConfig(cfg: Config, expected: Config) extends ConfigCmd
  case class MergeConfig(cfg: Config) extends ConfigCmd
  case class CheckAndMergeConfig(cfg: Config, expected: Config) extends ConfigCmd

  case class LoadResourceModule(resourceModule: ResourceModule)
  case class UnloadResourceModule(name: String)

  case object GetConfig extends ConfigCmd

  case class ConfigResponse(cfg: Config)

  sealed trait PrimeResourceEvent

}

class PrimeResourceActor(id: String) extends PersistentActor {

  override def persistenceId: String = "prime:" + id

  override def receiveCommand: Receive = logMsg("Command") andThen receivedCommand

  override def receiveRecover: Receive = logMsg("Recover") andThen receivedRecover

  def receivedCommand: Receive = {
    case _ =>
  }

  def receivedRecover: Receive = {
    case _ =>
  }

  def logMsg(msg: String): Receive = {
    case a: Any => println("$msg: Received command")
  }
}
