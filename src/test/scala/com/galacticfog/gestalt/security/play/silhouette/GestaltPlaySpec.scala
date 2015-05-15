package com.galacticfog.gestalt.security.play.silhouette

import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.matcher.{Matcher, JsonMatchers}
import org.junit.runner._
import play.api.libs.json.Json

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification {

  "gestalt-play-silhouette" should {

    "do something" in {
      ko("write me")
    }.pendingUntilFixed

    "injection crap" in new WithApplication(app = FakeApplication(

    )) {

    }

  }
}
