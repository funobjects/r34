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

import java.io.File
import java.net.URL
import java.nio.file.{Path, FileSystems, Files, Paths}

import akka.actor.{PoisonPill, Terminated, ActorRef}
import org.funobjects.r34.{Issue, ResourceModule, ActorTestKitSpec}
import org.scalactic.{Bad, Every, Good}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues._

import scala.concurrent.Await

//import scala.collection.JavaConversions._
import scala.concurrent.duration._

/**
 * Tests for dynamic module loading.
 */
class LoaderModuleSpec extends ActorTestKitSpec with BeforeAndAfterEach {

  val loader = new LoaderModule()
  var loaderRef: Option[ActorRef] = None


  override protected def beforeEach(): Unit = {
    // start a new module actor for each test
    super.beforeEach()
    loaderRef = loader.start()
  }

  override protected def afterEach(): Unit = {
    // stop the module actor, wait until termination (otherwise,
    // there is a race when starting another actor for the next text,
    // which could fail due to duplicate actor name)

    loaderRef foreach { ref =>
      watch(ref)
      system.stop(ref)
      expectMsgClass(classOf[Terminated])
      // Because module actors are top-level actors, there is still a race here -- the following is a workaround that
      // currently seems to work, but is not provable via actor contract; it depends on the assumption that if
      // we interact with the user supervisor with any message (in this case, an actorRef lookup), it will have
      // completed the processing of the Terminate message before we get our response.  That is not a safe assumption,
      // and a better way should be found.  This is only a problem because module actors have well-known names,
      // a design decision that may change in the future.  If so, this problem can be eliminated with unique actor names.
      system.actorSelection("/user").resolveOne(1.second)
    }
    super.beforeEach()
  }

  "LoaderModule" should {
    "load a module by url and class name" in {

      targetJarFor("testModule") map { file =>

        val loaderSel = system.actorSelection(s"/user/loader")
        val lookedUpLoaderRef = Await.result(loaderSel.resolveOne(4.seconds), 5.seconds)

        // create load request
        val url = new URL(s"file://${file.getAbsolutePath}")
        val loadMsg = LoaderModule.LoadOne(url, "org.funobjects.r34.modules.TestModule", system, exec, mat)
        lookedUpLoaderRef ! loadMsg
        val inst: LoaderModule.LoadOneResponse = expectMsgClass(classOf[LoaderModule.LoadOneResponse])
        inst.result shouldBe a [Good[_, _]]

        val testSel = system.actorSelection(s"/user/test")
        val testRef = Await.result(testSel.resolveOne(4.seconds), 5.seconds)

        testRef ! "ping"
        testRef ! PoisonPill
        expectMsg("pong")
      } getOrElse {
        // jar not built, so just skip test
        cancel("TestModule jar is not available; to enable this test, run 'sbt package' and test again.")
      }
    }

    "load modules indirectly via META-INF/r34modules" in {

      targetJarFor("testModule") map { file =>

        val loaderSel = system.actorSelection(s"/user/loader")
        val lookedUpLoaderRef = Await.result(loaderSel.resolveOne(4.seconds), 5.seconds)

        // create load request
        val url = new URL(s"file://${file.getAbsolutePath}")
        val loadMsg = LoaderModule.LoadAll(url, system, exec, mat)
        lookedUpLoaderRef ! loadMsg

        val inst: LoaderModule.LoadAllResponse = expectMsgClass(classOf[LoaderModule.LoadAllResponse])
        inst.result match {
          case Good(mods) => mods.length shouldBe 2
          case Bad(issue) => fail("Expected module list, got Issue instead: " + issue)
        }

        val testSel = system.actorSelection(s"/user/test")
        val testRef = Await.result(testSel.resolveOne(4.seconds), 5.seconds)

        val testSel2 = system.actorSelection(s"/user/test2")
        val testRef2 = Await.result(testSel2.resolveOne(4.seconds), 5.seconds)

        testRef ! "ping"
        testRef ! PoisonPill
        expectMsg("pong")

        testRef2 ! "ping2"
        testRef2 ! PoisonPill
        expectMsg("pong2")

      } getOrElse {
        // jar not built, so just skip test
        cancel("TestModule jar is not available; to enable this test, run 'sbt package' and test again.")
      }
    }

    "return an error when trying to load an invalid module" in {

      targetJarFor("invalidModule") map { file =>

        // create load request
        val url = new URL(s"file://${file.getAbsolutePath}")
        val loadAllMsg = LoaderModule.LoadAll(url, system, exec, mat)

        loaderRef.value ! loadAllMsg
        val loadAllRes: LoaderModule.LoadAllResponse = expectMsgClass(classOf[LoaderModule.LoadAllResponse])
        loadAllRes.result shouldBe a[Bad[_, _]]

        val loadOneMsg = LoaderModule.LoadOne(url, "some.class.name", system, exec, mat)
        loaderRef.value ! loadOneMsg
        val loadOneRes: LoaderModule.LoadOneResponse = expectMsgClass(classOf[LoaderModule.LoadOneResponse])
        loadOneRes.result shouldBe a[Bad[_, _]]
      } getOrElse {
        // jar not built, so just skip test
        cancel("TestModule jar is not available; to enable this test, run 'sbt package' and test again.")
      }
    }

    "return an error when trying to load a non-existent jar" in {
      // create load request
      val url = new URL(s"file:///probably/doesnt/exists")
      val loadMsg = LoaderModule.LoadAll(url, system, exec, mat)

      loaderRef.value ! loadMsg
      val inst: LoaderModule.LoadAllResponse = expectMsgClass(classOf[LoaderModule.LoadAllResponse])
      inst.result shouldBe a [Bad[_, _]]
    }

//    "return an error for a non-existent jar name" in { pending }
//    "return an error for a jar without a compatible module class" in { pending }

    /** get the path to the target directory for the given sub-project */
    def targetJarFor(proj: String): Option[File] = {
      // first, figure out the path to the jar file
      val versionR = """^(\d+)\.(\d+)\.(\d+)$""".r
      val thisVersion = util.Properties.versionNumberString

      val scalaBase =  thisVersion match {
        case versionR(maj, min, mic) => s"$maj.$min"
        case _ => fail("Unable to get version base from version string: " + thisVersion)
      }

      Paths.get(proj, "target", s"scala-$scalaBase")
        .toFile
        .listFiles()
        .find(_.getName.endsWith(".jar"))
    }
  }
}
