package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.fakes.{FakeGestaltFrameworkSecurityEnvironment, FakeGestaltSecurityModule}
import com.google.inject.Inject
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class TestFrameworkController @Inject()(messagesApi: MessagesApi,
                                        security: GestaltFrameworkSecurity) extends Controller {

  val sec = security

  def Authenticate() = new sec.GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => None: Option[UUID]}))
  def Authenticate(fqon: String) = new sec.GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => Some(fqon)}))
  def Authenticate(org: UUID) = new sec.GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => Some(org)}))
  def MaybeAuthenticate = sec.UserAwareAction()

  def hello = Authenticate() { securedRequest =>
    Ok(s"hello, ${securedRequest.identity.account.username}. Your credentials were\n${securedRequest.identity.creds}")
  }

  def someCallToSecurityWithUserCredentials = Authenticate().async { request =>
    GestaltOrg.getCurrentOrg()(sec.securityClient.withCreds(request.identity.creds)) map { org =>
      Ok(s"credentials validate against ${org.fqon}")
    }
  }

  def someUserAwareCall = MaybeAuthenticate {
    userAwareRequest =>
      userAwareRequest.identity.fold (
        Ok("hello, stranger")
      ) (
        user => Ok(s"hello, ${user.account.username}")
      )
  }

}

@RunWith(classOf[JUnitRunner])
class FakeFrameworkRequestSpec extends PlaySpecification with Mockito {

  def uuid() = UUID.randomUUID()

  def dummyCreds(authHeader: Option[String] = None): GestaltAPICredentials = {
    val header = authHeader getOrElse s"Bearer ${uuid()}"
    GestaltAPICredentials.getCredentials(header).get
  }

  def dummyAuthAccount(groups: Seq[ResourceLink] = Seq(), orgId: UUID = uuid()): GestaltAuthResponseWithCreds = {
    val directory = GestaltDirectory(uuid(), "test-directory", None, uuid())
    val account = GestaltAccount(
      id = uuid(),
      username = "someUser",
      firstName = "John",
      lastName = "Doe",
      description = Option("a crash-test dummy"),
      email = Option("john@doe.com"),
      phoneNumber = Option("+15058675309"),
      directory = directory
    )
    new GestaltAuthResponseWithCreds(
      account = account,
      groups = groups,
      rights = Seq(),
      orgId = orgId,
      creds = dummyCreds(),
      extraData = None
    )
  }

  lazy val testAuthResponse = dummyAuthAccount()
  lazy val testCreds = testAuthResponse.creds
  def fakeEnv = FakeGestaltFrameworkSecurityEnvironment(
    identities = Seq( testCreds -> testAuthResponse ),
    securityConfig = GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE, HTTP, "localhost", 9455, "empty", "empty", None, None),
    securityClient = mock[GestaltSecurityClient]
  )

  def app: Application =
    new GuiceApplicationBuilder()
      .bindings(
        FakeGestaltSecurityModule(fakeEnv)
      )
      .build

  abstract class WithFakeSecurity extends WithApplication(app) {
  }

  "FakeGestaltSecurityEnvironment" should {

    "support faked authorization" in new WithFakeSecurity {
      val request = FakeRequest().withHeaders(AUTHORIZATION -> testCreds.headerValue)
      val controller = app.injector.instanceOf[TestFrameworkController]
      val result = controller.hello(request)

      status(result) must equalTo(OK)
      contentAsString(result) must startWith("hello, " + testAuthResponse.account.username)
    }

    "handle user aware actions" in new WithFakeSecurity {
      val controller = app.injector.instanceOf[TestFrameworkController]

      val resultAuth = controller.someUserAwareCall(FakeRequest().withHeaders(AUTHORIZATION -> testCreds.headerValue))
      status(resultAuth) must equalTo(OK)
      contentAsString(resultAuth) must startWith("hello, " + testAuthResponse.account.username)

      val resultAnon = controller.someUserAwareCall(FakeRequest())
      status(resultAnon) must equalTo(OK)
      contentAsString(resultAnon) must startWith("hello, stranger")
    }

    "401 if no registered header even during faked authorization" in new WithFakeSecurity {
      val request = FakeRequest() // no .withHeaders(AUTHORIZATION -> creds.headerValue)
      val controller = app.injector.instanceOf[TestFrameworkController]
      val result = controller.hello(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "support mocked GestaltSecurityClient" in new WithFakeSecurity {
      import com.galacticfog.gestalt.security.api.json.JsonImports.orgFormat
      val org = GestaltOrg(UUID.randomUUID(), "some-org", fqon = "some-org", None, None, Seq())

      val controller = app.injector.instanceOf[TestFrameworkController]
      val mc2 = mock[GestaltSecurityClient]
      controller.sec.securityClient.withCreds(testCreds) returns mc2
      // this sucks... testers should have to know the REST API, but GestaltOrg.getCurrentOrg is a static method and therefore a pain to mock
      mc2.get[GestaltOrg]("orgs/current") returns Future.successful(org)

      val request = FakeRequest().withHeaders(AUTHORIZATION -> testCreds.headerValue)
      val result = controller.someCallToSecurityWithUserCredentials(request)

      status(result) must equalTo(OK)
      contentAsString(result) must contain(org.fqon)
    }

  }

}
