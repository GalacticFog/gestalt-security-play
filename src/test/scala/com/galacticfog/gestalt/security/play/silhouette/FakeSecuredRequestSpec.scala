package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.test.FakeGestaltSecurityEnvironment
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import play.api.mvc.RequestHeader
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FakeSecuredRequestSpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  // this is how gestalt-meta uses GestaltFrameworkSecuredController, so it's a good test/example case
  trait SecureController extends GestaltFrameworkSecuredController[DummyAuthenticator] {
    override def getAuthenticator: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService

    def Authenticate() = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => None: Option[UUID]}))
    def Authenticate(fqon: String) = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => Some(fqon)}))
    def Authenticate(org: UUID) = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => Some(org)}))
  }

  class TestController extends SecureController {

    // this doesn't really matter, because it's used only to construct an internal environment in the base class
    // we'll override it in our tests below
    override def getAuthenticator = throw new RuntimeException("cannot instantiate TestController directly, must subclass and override env")

    def hello = Authenticate() { securedRequest =>
      Ok(s"hello, ${securedRequest.identity.account.username}. Your credentials were\n${securedRequest.identity.creds}")
    }

    def someDelegatedCallToSecurity = Authenticate().async { request =>
      GestaltOrg.getCurrentOrg()(securityClient.withCreds(request.identity.creds)) map { org =>
        Ok(s"credentials validate against ${org.fqon}")
      }
    }
  }

  def uuid() = UUID.randomUUID()

  def dummyAuthAccount(userInfo: Map[String,String] = Map(), groups: Seq[ResourceLink] = Seq(), orgId: UUID = uuid()): GestaltAuthResponse = {
    val defaultStr = "foo"
    val directory = GestaltDirectory(uuid(), defaultStr, None, uuid())
    val account = GestaltAccount(
      userInfo.get("id") map (UUID.fromString(_)) getOrElse uuid(),
      userInfo.getOrElse("username", defaultStr),
      userInfo.getOrElse("firstName", defaultStr),
      userInfo.getOrElse("lastName", defaultStr),
      userInfo.get("description") orElse Option(defaultStr),
      userInfo.get("email") orElse Option(defaultStr),
      userInfo.get("phoneNumber") orElse Option(defaultStr),
      directory
    )
    GestaltAuthResponse(
      account = account,
      groups = groups,
      rights = Seq(),
      orgId = orgId
    )
  }

  def dummyCreds(authHeader: Option[String] = None): GestaltAPICredentials = {
    val header = authHeader getOrElse s"Bearer ${uuid()}"
    GestaltAPICredentials.getCredentials(header).get
  }

  "FakeGestaltSecurityEnvironment" should {

    val creds = dummyCreds()
    val authResponse = dummyAuthAccount()
    val fakeEnv = FakeGestaltSecurityEnvironment[DummyAuthenticator](Seq(
      creds -> authResponse
    ), mock[GestaltSecurityClient])

    "support faked authorization" in new WithApplication {
      val request = FakeRequest().withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = new TestController {
        override val env = fakeEnv
      }
      val result = controller.hello(request)

      status(result) must equalTo(OK)
      contentAsString(result) must startWith("hello, " + authResponse.account.username)
    }

    "401 if no registered header even during faked authorization" in new WithApplication {
      val request = FakeRequest() // no .withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = new TestController {
        override val env = fakeEnv
      }
      val result = controller.hello(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "support mocked GestaltSecurityClient" in new WithApplication {
      import com.galacticfog.gestalt.security.api.json.JsonImports.orgFormat
      val org = GestaltOrg(UUID.randomUUID(), "some-org", fqon = "some-org", None, None, Seq())
      val mc1 = mock[GestaltSecurityClient]
      val mc2 = mock[GestaltSecurityClient]
      mc1.withCreds(creds) returns mc2
      // this sucks... testers should have to know the REST API, but GestaltOrg.getCurrentOrg is a static method
      mc2.get[GestaltOrg]("orgs/current") returns Future.successful(org)
      val request = FakeRequest().withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = new TestController {
        override val env = fakeEnv
        override implicit val securityClient: GestaltSecurityClient = mc1
      }
      val result = controller.someDelegatedCallToSecurity(request)

      status(result) must equalTo(OK)
      contentAsString(result) must contain(org.fqon)
    }

  }

}
