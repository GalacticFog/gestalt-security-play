package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.examples.ExampleInstanceController
import com.galacticfog.gestalt.security.play.silhouette.utils.{GestaltSecurityConfig, GestaltSecurityModule}
import com.google.inject.Guice
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test.WithApplication

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification {

  val testConfig = GestaltSecurityConfig(
    protocol = HTTP,
    host = "test.host.com",
    port = 1234,
    apiKey = "someKey",
    apiSecret = "someSecret",
    appId = Some("appId")
  )

  "GestaltSecurityModule" should {

    "allow override on getSecurityConfig" in {
      val overrideModule = new GestaltSecurityModule {
        override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(testConfig)
      }

      overrideModule.getSecurityConfig must beSome(testConfig)

      val injector = Guice.createInjector(overrideModule)
      val instance = injector.getInstance(classOf[GestaltSecurityConfig])
      instance must be(testConfig)
    }

  }

  "GestaltSecuredInstanceController" should {

    class TestInstanceController(config: GestaltSecurityConfig) extends GestaltSecuredInstanceController with ExampleInstanceController {
      override def getSecurityConfig: GestaltSecurityConfig = config
    }

    // requires WithApplication to create wsclient
    "allow easy specification of config" in new WithApplication {
      val controller = new TestInstanceController(testConfig)
      controller.client.protocol must_== testConfig.protocol
      controller.client.hostname must_== testConfig.host
      controller.client.port must_== testConfig.port
      controller.client.apiKey must_== testConfig.apiKey
      controller.client.apiSecret must_== testConfig.apiSecret
    }

  }

}
