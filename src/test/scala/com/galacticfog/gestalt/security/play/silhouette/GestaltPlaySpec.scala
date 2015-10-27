package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.io.GestaltConfig
import com.galacticfog.gestalt.io.GestaltConfig.GestaltContext
import com.galacticfog.gestalt.io.internal.ContextLoader
import com.galacticfog.gestalt.meta.play.utils.GlobalMeta
import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, HTTP, GestaltSecurityConfig}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
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
    appId = Some(UUID.randomUUID)
  )

  "GestaltSecuredInstanceController" should {

    class TestControllerWithConfigOverride(config: GestaltSecurityConfig) extends GestaltSecuredController {
      override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(config)
    }

    class TestFrameworkControllerSecurity extends GestaltFrameworkSecuredController[DummyAuthenticator] {
      override def getAuthenticator: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService

      // define a function based on the request header
      def inSitu() = GestaltFrameworkAuthAction({rh: RequestHeader => Some(rh.path)}) {
        securedRequest => Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds)
      }

      // like above, but async+json
      def inSituJsonAsync() = GestaltFrameworkAuthAction({rh: RequestHeader => Some(rh.path)}).async(parse.json) {
        securedRequest => Future{Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds)}
      }

      // just pass a string
      def fromArgs(fqon: String) = GestaltFrameworkAuthAction(Some(fqon)) {
        securedRequest => Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds + " on org " + fqon)
      }

      // just pass a string, async+json
      def asyncJson() = GestaltFrameworkAuthAction(Some("test.org")).async(parse.json) {
        securedRequest => Future{Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds)}
      }

      // how about a UUID? we got that covered! this authenticates against /orgs/:orgId/auth
      def aUUID(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)).async(parse.json) {
        securedRequest => Future{Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds)}
      }

    }

    // requires WithApplication to create wsclient
    "allow easy specification of config via override" in new WithApplication {
      val controller = new TestControllerWithConfigOverride(testConfig)
      controller.securityConfig.protocol   must_== testConfig.protocol
      controller.securityConfig.hostname   must_== testConfig.hostname
      controller.securityConfig.port       must_== testConfig.port
      controller.securityConfig.apiKey     must_== testConfig.apiKey
      controller.securityConfig.apiSecret  must_== testConfig.apiSecret
      controller.securityConfig.appId      must_== testConfig.appId
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
      controller.securityClient.apiKey     must_== testConfig.apiKey.get
      controller.securityClient.apiSecret  must_== testConfig.apiSecret.get
      controller.securityClient.protocol   must_== testConfig.protocol
      controller.securityClient.hostname   must_== testConfig.hostname

      controller.securityConfig.apiKey     must_== testConfig.apiKey
      controller.securityConfig.apiSecret  must_== testConfig.apiSecret
      controller.securityConfig.protocol   must_== testConfig.protocol
      controller.securityConfig.hostname   must_== testConfig.hostname
      controller.securityConfig.appId      must_== testConfig.appId
    }

  }

}
