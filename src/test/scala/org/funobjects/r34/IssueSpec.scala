package org.funobjects.r34

import org.scalatest.{Matchers, WordSpec}

/**
 * Created by rgf on 11/1/15.
 */
class IssueSpec extends WordSpec with Matchers {

  "Issue" should {
    "be able to be created from a simple string" in {
      val issue = Issue("Hello there.")
      issue.toString shouldBe "Hello there."

    }
    "be able to be created from a string with parameters" in {
      Issue("This is a %1$s", Array("test.")).toString shouldBe "This is a test."
    }
    "be able to be created from a string and exception" in {
      val ex = new Exception("foo bar")
      val issue = Issue("This is a test.", ex)
      issue.toString shouldBe "This is a test. [foo bar]"
    }
    "include related issues" in {
      val issue1 = Issue("Issue 1")
      val issue2 = Issue("Issue 2", new Exception("Boo!"))
      val issue3 = Issue("Issue 3")

      val both = issue1 :+ issue2
      both.toString shouldBe "Issue 1\n    Issue 2 [Boo!]"
      (both :+ issue3).toString shouldBe "Issue 1\n    Issue 2 [Boo!]\n    Issue 3"

      val all = issue1 ++ Seq(issue2, issue3)
      all.toString shouldBe "Issue 1\n    Issue 2 [Boo!]\n    Issue 3"
    }
  }
}
