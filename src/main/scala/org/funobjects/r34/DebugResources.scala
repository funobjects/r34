/*
 *  Copyright 2015 Functional Objects, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.funobjects.r34

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Extra resources for debugging the system.  Should NOT be enabled in production.
 *
 * @author Robert Fries
 */
class DebugResources(implicit sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends ResourceModule()(sys, exec, flows) {
  override val name: String = "debug"
  override val routes: Option[Route] = Some(debugRoutes)

  def debugRoutes: Route = {
    (get & path("clock")) {
      complete {
        HttpResponse(StatusCodes.OK,
          entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`,
            Source(0.seconds, 15.seconds, 0)
              .mapMaterialized(c => ())
              .via(Flow[Int].map(tick => HttpEntity.ChunkStreamPart(s"${new java.util.Date}\n")))
          )
        )
      }

    }
  }
}
