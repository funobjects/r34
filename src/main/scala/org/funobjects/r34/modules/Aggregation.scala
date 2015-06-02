package org.funobjects.r34.modules

import akka.actor.Actor.Receive
import akka.actor._
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
 * Aggregate module container.
 *
 * @author Robert Fries
 */
class Aggregation(id: String, modules: List[ResourceModule])(implicit val sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {

  override val name = "root"
  override val routes = Some(aggregateRoutes)
  override val props = Some(Props[Aggregation.AggregatingActor])

  val moduleMap = modules.map(mod => (mod.name, mod)).toMap

  lazy val aggregateRoutes: Route = {
    path(Segment) { resName =>
      moduleMap.get(resName).flatMap(_.routes).getOrElse(reject)
    }
  }
}

object Aggregation {

  def props(modules: List[ResourceModule]) = Props(classOf[AggregatingActor], modules)

  class AggregatingActor(modules: List[ResourceModule]) extends Actor {

    var refs: Map[ActorRef, ResourceModule] = Map.empty

    var unhandled = 0

    override def preStart(): Unit = {
      // side-effect some actors into being...

      refs = (
        for (module <- modules;
             props <- module.props)
          yield (context.system.actorOf(props, "module:" + module.name), module)
        ).toMap
    }

    override def receive: Actor.Receive = {
      case Terminated(ref) =>
        refs -= ref
        if (refs.isEmpty) {
          context.stop(self)
        }
      case _ => unhandled += 1
    }
  }
}