package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityConfig
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test.WithApplication

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

  "GestaltSecuredInstanceController" should {

    class TestInstanceController(config: GestaltSecurityConfig) extends GestaltSecuredController {
      override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(config)
    }

    // requires WithApplication to create wsclient
    "allow easy specification of config" in new WithApplication {
      val controller = new TestInstanceController(testConfig)
      controller.config.protocol  must_== testConfig.protocol
      controller.config.host      must_== testConfig.host
      controller.config.port      must_== testConfig.port
      controller.config.apiKey    must_== testConfig.apiKey
      controller.config.apiSecret must_== testConfig.apiSecret
      controller.config.appId     must_== testConfig.appId
    }

  }

}
