/*
 * Copyright 2015 Functional Objects, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.funobjects.r34.modules

import java.net.URL
import java.nio.file.{FileSystems, Files, Paths}

import org.funobjects.r34.{Issue, ResourceModule, ActorTestKitSpec}
import org.scalactic.{Every, Good}
import org.scalatest.OptionValues._

import scala.concurrent.Await

//import scala.collection.JavaConversions._
import scala.concurrent.duration._

/**
 * Tests for dynamic module loading.
 */
class LoaderModuleSpec extends ActorTestKitSpec {

  val loader = new LoaderModule()
  val loaderRef = loader.start()

  "LoaderModule" should {
    "load a module and successfully obtain a reference to it" in {

      // first, figure out the path to the jar file
      val versionR = """^(\d+)\.(\d+)\.(\d+)$""".r
      val thisVersion = util.Properties.versionNumberString

      val scalaBase =  thisVersion match {
        case versionR(maj, min, mic) => s"$maj.$min"
        case _ => fail("Unable to get version base from version string: " + thisVersion)
      }

      val targetPath = Paths.get("/Users", "rgf", "git", "r34",  "testModule", "target", s"scala-$scalaBase")

      val maybeFile = targetPath.toFile.listFiles().find(_.getName.endsWith(".jar"))
      maybeFile map { file =>

        val loaderSel = system.actorSelection(s"/user/loader")
        val lookedUpLoaderRef = Await.result(loaderSel.resolveOne(4.seconds), 5.seconds)

        // create load request
        val url = new URL(s"file://${file.getPath}")
        val loadMsg = LoaderModule.Load(url, "org.funobjects.r34.modules.TestModule", system, exec, mat)
        lookedUpLoaderRef ! loadMsg
        val inst: LoaderModule.Instance = expectMsgClass(classOf[LoaderModule.Instance])
        inst.result shouldBe a [Good[_, _]]

        println("instance: " + inst)
        ""
      } getOrElse {
        // jar not built, so just skip test
        cancel("TestModule jar is not available; to enable this test, run 'sbt package' and test again.")
      }

      //println(s"path: ${targetPath.toString} file: ${file.value.getPath} exists: ${file.value.exists}")
    }

    "return an error for a non-existent jar name" in { pending }
    "return an error for a jar without a compatible module class" in { pending }
  }
}
