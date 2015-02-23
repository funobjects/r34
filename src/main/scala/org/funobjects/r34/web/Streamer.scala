package org.funobjects.r34.web

import akka.actor.ActorSystem
import akka.http.model._
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Random

/**
 * Example of connecting a route to a stream.
 */

object Streamer {

  class SlowBoundedIterator extends Iterator[Int] {
    var list = (1 to 5).reverse
    override def hasNext: Boolean = list.nonEmpty
    override def next(): Int = {
      val n = list.head
      list = list.tail
      Thread.sleep(1000)
      n
    }
  }

  val slowSrc = Source(() => new SlowBoundedIterator).map(n => HttpEntity.ChunkStreamPart(s"$n\n"))

  def routes(implicit sys: ActorSystem, flows: FlowMaterializer, executionContext: ExecutionContext) = {
    path("slow" ) {
      complete {
        HttpResponse(StatusCodes.OK, entity = HttpEntity.Chunked(ContentTypes.`text/plain`, slowSrc))
      }
    }
  }
}

