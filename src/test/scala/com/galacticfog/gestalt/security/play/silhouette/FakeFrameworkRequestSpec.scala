package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.test.FakeGestaltSecurityEnvironment
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.junit.runner._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.inject.bind

import scala.concurrent.Future
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.Application

@RunWith(classOf[JUnitRunner])
class FakeFrameworkRequestSpec extends PlaySpecification with Mockito {

  class TestController(messagesApi: MessagesApi,
                       env: GestaltFrameworkSecurityEnvironment[DummyAuthenticator])
    extends GestaltFrameworkSecuredController[DummyAuthenticator](messagesApi, env) {

    def Authenticate() = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => None: Option[UUID]}))
    def Authenticate(fqon: String) = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => Some(fqon)}))
    def Authenticate(org: UUID) = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => Some(org)}))

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

  def dummyAuthAccount(groups: Seq[ResourceLink] = Seq(), orgId: UUID = uuid()): GestaltAuthResponse = {
    val defaultStr = "foo"
    val directory = GestaltDirectory(uuid(), defaultStr, None, uuid())
    val account = GestaltAccount(
      id = uuid(),
      username = defaultStr,
      firstName = defaultStr,
      lastName = defaultStr,
      description = Option(defaultStr),
      email = Option(defaultStr),
      phoneNumber = Option(defaultStr),
      directory = directory
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

  abstract class FakeSecurity extends WithApplication {
    val creds = dummyCreds()
    val authResponse = dummyAuthAccount()
    val fakeEnv = FakeGestaltSecurityEnvironment[DummyAuthenticator](
      identities = Seq( creds -> authResponse ),
      config = mock[GestaltSecurityConfig],
      client = mock[GestaltSecurityClient]
    )

    override implicit def implicitApp: Application = {
      new GuiceApplicationBuilder()
        .overrides(bind[GestaltSecurityEnvironment[AuthAccountWithCreds,DummyAuthenticator]].toInstance(fakeEnv))
        .build
    }
  }

  "FakeGestaltSecurityEnvironment" should {



    "support faked authorization" in new FakeSecurity {
      val request = FakeRequest().withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = app.injector.instanceOf[TestController]
      val result = controller.hello(request)

      status(result) must equalTo(OK)
      contentAsString(result) must startWith("hello, " + authResponse.account.username)
    }

    "401 if no registered header even during faked authorization" in new FakeSecurity {
      val request = FakeRequest() // no .withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = app.injector.instanceOf[TestController]
      val result = controller.hello(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "support mocked GestaltSecurityClient" in new FakeSecurity {
      import com.galacticfog.gestalt.security.api.json.JsonImports.orgFormat
      val org = GestaltOrg(UUID.randomUUID(), "some-org", fqon = "some-org", None, None, Seq())
      val mc1 = mock[GestaltSecurityClient]
      val mc2 = mock[GestaltSecurityClient]
      mc1.withCreds(creds) returns mc2
      // this sucks... testers should have to know the REST API, but GestaltOrg.getCurrentOrg is a static method
      mc2.get[GestaltOrg]("orgs/current") returns Future.successful(org)
      val request = FakeRequest().withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = app.injector.instanceOf[TestController]
      val result = controller.someDelegatedCallToSecurity(request)

      status(result) must equalTo(OK)
      contentAsString(result) must contain(org.fqon)
    }

  }

}
