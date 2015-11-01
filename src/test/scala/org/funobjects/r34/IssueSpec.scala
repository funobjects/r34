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
    "be able to be created from a string with paramters" in {
      Issue("This is a %1$s", Array("test.")).toString shouldBe "This is a test."
    }
    "be able to be created from a string and exception" in {
      val ex = new Exception("foo bar")
      val issue = Issue("This is a test.", ex)
      issue.toString shouldBe "This is a test.\nfoo bar"
    }
  }
}
