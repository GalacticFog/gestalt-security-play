package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.io.GestaltConfig
import com.galacticfog.gestalt.io.GestaltConfig.GestaltContext
import com.galacticfog.gestalt.io.internal.{Ok, ContextLoader}
import com.galacticfog.gestalt.meta.play.utils.GlobalMeta
import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, HTTP, GestaltSecurityConfig}
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Action}
import play.api.test.WithApplication

import scala.concurrent.Future
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification with Mockito {

  val testConfig = GestaltSecurityConfig(
    mode = DELEGATED_SECURITY_MODE,
    protocol = HTTP,
    hostname = "test.host.com",
    port = 1234,
    apiKey = Some("someKey"),
    apiSecret = Some("someSecret"),
    appId = Some("appId")
  )

  "GestaltSecuredInstanceController" should {

    class TestControllerWithConfigOverride(config: GestaltSecurityConfig) extends GestaltSecuredController {
      override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(config)
    }

    class TestControllerWithOrgRequest extends GestaltSecuredController {
      def home() = Action.async { implicit request =>
        GestaltFrameworkAuthRequest { "test.root" } {
          securedRequest => Ok(securedRequest.identity.account.id.toString)
        }
      }

      def jsonPost() = Action.async(parse.json) { implicit request =>
        GestaltFrameworkAuthRequest(request) { "test.root" } {
          securedRequest => Ok(securedRequest.identity.account.id.toString)
        }
      }
    }

    // requires WithApplication to create wsclient
    "allow easy specification of config via override" in new WithApplication {
      val controller = new TestControllerWithConfigOverride(testConfig)
      controller.securityConfig.protocol  must_== testConfig.protocol
      controller.securityConfig.hostname  must_== testConfig.hostname
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
      val cl = mock[ContextLoader]
      meta.local returns cl
      cl.context returns Some(GestaltContext("","","","",0,None,GestaltConfig.Environment("",""),""))
      val controller = new TestController(meta)
      controller.securityClient.apiKey    must_== testConfig.apiKey.get
      controller.securityClient.apiSecret must_== testConfig.apiSecret.get
      controller.securityClient.protocol  must_== testConfig.protocol
      controller.securityClient.hostname  must_== testConfig.hostname

      controller.securityConfig.apiKey    must_== testConfig.apiKey
      controller.securityConfig.apiSecret must_== testConfig.apiSecret
      controller.securityConfig.protocol  must_== testConfig.protocol
      controller.securityConfig.hostname  must_== testConfig.hostname
      controller.securityConfig.appId     must_== testConfig.appId
    }

  }

}
