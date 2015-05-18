package org.funobjects.r34

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.persistence.PersistentActor
import akka.stream.FlowMaterializer

import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * The prime resource of the system.
 *
 * @author Robert Fries
 */
class PrimeResourceModule(id: String)(implicit val sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {

  override val name = "prime"

  override val props: Option[Props] = Some(Props(classOf[PrimeResourceActor], id))
  override val routes: Option[Route] = Some(primeRoutes)

  def primeRoutes: Route = {
    logRequestResult("r34") {
      path("shutdown") {
        complete {
          sys.scheduler.scheduleOnce(1.second) { sys.shutdown() }
          HttpResponse(StatusCodes.OK)
        }
      }
    }
  }
}

object PrimeResourceModule {

  sealed trait PrimeResourceCmd

  case class Shutdown(reason: String = "None")  extends PrimeResourceCmd
  case class NewConfig(cfg: Config) extends PrimeResourceCmd
  case class NewConfigIfMatching(cfg: Config, expected: Config) extends PrimeResourceCmd
  case class MergeConfig(cfg: Config) extends PrimeResourceCmd
  case class MergeConfigIfMatching(cfg: Config, expected: Config) extends PrimeResourceCmd

  case class LoadResourceModule(resourceModule: ResourceModule)
  case class UnloadResourceModule(name: String)

  case object GetConfig extends PrimeResourceCmd

  case class ConfigResponse(cfg: Config)

  sealed trait Ev

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
