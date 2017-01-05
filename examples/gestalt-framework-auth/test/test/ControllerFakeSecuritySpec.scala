package test

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthResponseWithCreds
import com.galacticfog.gestalt.security.play.silhouette.fakes.{FakeGestaltFrameworkSecurityEnvironment, FakeGestaltSecurityModule}
import com.galacticfog.gestalt.security.play.silhouette.modules.{GestaltFrameworkSecurityConfigModule, GestaltSecurityModule}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
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

@RunWith(classOf[JUnitRunner])
class ControllerFakeSecuritySpec extends PlaySpecification with Mockito {

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
      creds = dummyCreds()
    )
  }

  lazy val testAuthResponse = dummyAuthAccount()
  lazy val testCreds = testAuthResponse.creds
  def fakeEnv = FakeGestaltFrameworkSecurityEnvironment[DummyAuthenticator](
    identities = Seq( testCreds -> testAuthResponse ),
    config = GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE, HTTP, "localhost", 9455, "empty", "empty", None, None),
    client = mock[GestaltSecurityClient]
  )

  def app: Application =
    new GuiceApplicationBuilder()
      .disable( // disable the usual security modules
        classOf[GestaltFrameworkSecurityConfigModule],
        classOf[GestaltSecurityModule]
      )
      .bindings( // enable the fake security module using the fake environment created by fakeEnv
        FakeGestaltSecurityModule(fakeEnv)
      )
      .build

  abstract class WithFakeSecurity extends WithApplication(app) {
  }

}
