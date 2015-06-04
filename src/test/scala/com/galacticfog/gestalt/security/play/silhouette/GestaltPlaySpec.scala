package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.meta.play.utils.GlobalMeta
import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.api.GestaltSecurityConfig
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test.WithApplication

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification with Mockito {

  val testConfig = GestaltSecurityConfig(
    protocol = HTTP,
    host = "test.host.com",
    port = 1234,
    apiKey = "someKey",
    apiSecret = "someSecret",
    appId = Some("appId")
  )

  "GestaltSecuredInstanceController" should {

    class TestControllerWithConfigOverride(config: GestaltSecurityConfig) extends GestaltSecuredController {
      override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(config)
    }

    // requires WithApplication to create wsclient
    "allow easy specification of config via override" in new WithApplication {
      val controller = new TestControllerWithConfigOverride(testConfig)
      controller.securityConfig.protocol  must_== testConfig.protocol
      controller.securityConfig.host      must_== testConfig.host
      controller.securityConfig.port      must_== testConfig.port
      controller.securityConfig.apiKey    must_== testConfig.apiKey
      controller.securityConfig.apiSecret must_== testConfig.apiSecret
      controller.securityConfig.appId     must_== testConfig.appId
    }

    object TestGlobal extends GlobalSettings with GlobalMeta {}

    class TestController(meta: Gestalt) extends GestaltSecuredController(Some(meta)) {
    }

    "get config from meta by default" in new WithApplication {
      val meta = mock[Gestalt]
      meta.getConfig("authentication") returns Success(Json.toJson(testConfig).toString)
      val controller = new TestController(meta)
      controller.securityClient.apiKey    must_== testConfig.apiKey
      controller.securityClient.apiSecret must_== testConfig.apiSecret
      controller.securityClient.protocol  must_== testConfig.protocol
      controller.securityClient.hostname  must_== testConfig.host

      controller.securityConfig.apiKey    must_== testConfig.apiKey
      controller.securityConfig.apiSecret must_== testConfig.apiSecret
      controller.securityConfig.protocol  must_== testConfig.protocol
      controller.securityConfig.host      must_== testConfig.host
      controller.securityConfig.appId     must_== testConfig.appId
    }

  }

}
