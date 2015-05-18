package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.utils.{GestaltSecurityConfig, GestaltSecurityModule}
import com.google.inject.Guice
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification {

  "GestaltSecurityModule" should {

    "allow override on getSecurityConfig" in {
      val config = GestaltSecurityConfig(
        protocol = HTTP,
        host = "test.host.com",
        port = 1234,
        apiKey = "someKey",
        apiSecret = "someSecret",
        appId = Some("appId")
      )

      val overrideModule = new GestaltSecurityModule {
        override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(config)
      }

      overrideModule.getSecurityConfig must beSome(config)

      val injector = Guice.createInjector(overrideModule)
      val instance = injector.getInstance(classOf[GestaltSecurityConfig])
      instance must be(config)
    }

  }


}
