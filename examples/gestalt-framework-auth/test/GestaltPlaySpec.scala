package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID
import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.json.JsonImports.authFormat
import com.galacticfog.gestalt.security.api.json.JsonImports.tokenIntrospectionResponse
import com.galacticfog.gestalt.security.play.silhouette.fakes.{FakeGestaltFrameworkSecurityEnvironment, FakeGestaltFrameworkSecurityModule}
import com.google.inject.{AbstractModule, Inject}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import mockws.MockWS
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test._
import play.mvc.Http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._

class SecurityConfigModule(config: GestaltSecurityConfig) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[GestaltSecurityConfig]).toInstance(config)
  }
}



@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  val testConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "test.host.com",
    port = 1234,
    apiKey = "someKey",
    apiSecret = "someSecret",
    appId = None
  )

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
  lazy val fakeEnv = FakeGestaltFrameworkSecurityEnvironment[DummyAuthenticator](
    identities = Seq( testCreds -> testAuthResponse ),
    config = mock[GestaltSecurityConfig],
    client = mock[GestaltSecurityClient]
  )

  def app: Application =
    new GuiceApplicationBuilder()
      .bindings(
        new FakeGestaltFrameworkSecurityModule(fakeEnv)
      )
      .build

  abstract class FakeSecurity extends WithApplication(app) {
  }

  "GestaltFrameworkSecuredController" should {

    // requires WithApplication to create wsclient
    "allow easy specification of config via constructor injection" in new WithApplication {
      val controller = app.injector.instanceOf[TestSecuredController]
      controller.securityClient.protocol   must_== testConfig.protocol
      controller.securityClient.hostname   must_== testConfig.hostname
      controller.securityClient.port       must_== testConfig.port
      controller.securityClient.creds.asInstanceOf[GestaltBasicCredentials].username  must_== testConfig.apiKey
      controller.securityClient.creds.asInstanceOf[GestaltBasicCredentials].password  must_== testConfig.apiSecret
    }

    "return WWW-Authenticate header on 401 (root)" in new WithApplication {
      val controller = app.injector.instanceOf[TestSecuredController]
      val realm = s"${controller.securityClient.protocol}://${controller.securityClient.hostname}:${controller.securityClient.port}/root/oauth/issue"
      val result = await(controller.hello().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return WWW-Authenticate header on 401 (FQON)" in new WithApplication {
      val controller = app.injector.instanceOf[TestSecuredController]
      val fqon = "galacticfog"
      val realm = s"${controller.securityClient.protocol}://${controller.securityClient.hostname}:${controller.securityClient.port}/$fqon/oauth/issue"
      val result = await(controller.fromArgs(fqon).apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return WWW-Authenticate header on 401 (realm-override)" in new WithApplication() {
      val controller = app.injector.instanceOf[TestSecuredController]
      val realm = "https://realm.override:9455"
      val result = await(controller.hello().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return WWW-Authenticate header on 401 (UUID)" in new WithApplication {
      val controller = app.injector.instanceOf[TestSecuredController]
      val orgId = UUID.randomUUID()
      val realm = s"${controller.securityClient.protocol}://${controller.securityClient.hostname}:${controller.securityClient.port}/orgs/$orgId/oauth/issue"
      val result = await(controller.aUUID(orgId).apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

  }

  "GestaltFrameworkAuthProvider" should {

    "authenticate api credentials against org auth endpoints" in {
      val securityHostname = "security.local"
      val securityPort = 9455
      val url = s"http://${securityHostname}:${securityPort}/auth"
      val authResponse = GestaltAuthResponse(
        account = GestaltAccount(UUID.randomUUID(), "username", "", "", None, None, None, GestaltDirectory(
          UUID.randomUUID(), "name", None, UUID.randomUUID()
        )),
        groups = Seq(),
        rights = Seq(),
        orgId = UUID.randomUUID()
      )
      val ws = MockWS {
        case (POST,url) => Action { implicit request => Ok(Json.toJson(authResponse))}
      }
      val client = GestaltSecurityClient(ws, HTTP, securityHostname, securityPort, "not", "used")
      val creds = GestaltBasicCredentials(
        username = UUID.randomUUID().toString,
        password = "FQ1UeAvh/xWIwU7qZCA108A2C7ZqSx+8faeIOoYT"
      )
      val request = FakeRequest("GET", "securedEndpoint", FakeHeaders(
        Seq(AUTHORIZATION -> creds.headerValue)
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(None,request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beSome(authResponse)
    }

    "authenticate using remote validated tokens" in {
      val securityHostname = "security.local"
      val securityPort = 9455
      val url = s"http://${securityHostname}:${securityPort}/root/oauth/inspect"
      val token = OpaqueToken(UUID.randomUUID(), ACCESS_TOKEN)
      val NOT_USED = "nokey"
      val authResponse = GestaltAuthResponse(
        account = GestaltAccount(UUID.randomUUID(), "username", "", "", None, None, None, GestaltDirectory(
          UUID.randomUUID(), "name", None, UUID.randomUUID()
        )),
        groups = Seq(),
        rights = Seq(),
        orgId = UUID.randomUUID()
      )
      val introspectionResponse = ValidTokenResponse(
        username = "username",
        sub = authResponse.account.href,
        iss = "",
        exp = DateTime.now().getMillis / 1000,
        iat = DateTime.now().getMillis / 1000,
        jti = token.id,
        gestalt_account = authResponse.account,
        gestalt_token_href = s"/tokens/${token.id.toString}",
        gestalt_rights = authResponse.rights,
        gestalt_groups = authResponse.groups,
        gestalt_org_id = authResponse.orgId
      )
      val ws = MockWS {
        case (POST,url) => Action { implicit request => Ok(Json.toJson(introspectionResponse))}
      }
      val client = GestaltSecurityClient(ws, HTTP, securityHostname, securityPort, NOT_USED, NOT_USED)
      val creds = GestaltBearerCredentials(token.toString)
      val request = FakeRequest("GET", "securedEndpoint", FakeHeaders(
        Seq(AUTHORIZATION -> creds.headerValue)
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(Some("root"),request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beSome(authResponse)
      resp.get must beAnInstanceOf[GestaltAuthResponseWithCreds]
    }

    "authenticate using remote validated tokens with dcos syntax" in {
      val securityHostname = "security.local"
      val securityPort = 9455
      val url = s"http://${securityHostname}:${securityPort}/root/oauth/inspect"
      val token = OpaqueToken(UUID.randomUUID(), ACCESS_TOKEN)
      val NOT_USED = "nokey"
      val authResponse = GestaltAuthResponse(
        account = GestaltAccount(UUID.randomUUID(), "username", "", "", None, None, None, GestaltDirectory(
          UUID.randomUUID(), "name", None, UUID.randomUUID()
        )),
        groups = Seq(),
        rights = Seq(),
        orgId = UUID.randomUUID()
      )
      val introspectionResponse = ValidTokenResponse(
        username = "username",
        sub = authResponse.account.href,
        iss = "",
        exp = DateTime.now().getMillis / 1000,
        iat = DateTime.now().getMillis / 1000,
        jti = token.id,
        gestalt_account = authResponse.account,
        gestalt_token_href = s"/tokens/${token.id.toString}",
        gestalt_rights = authResponse.rights,
        gestalt_groups = authResponse.groups,
        gestalt_org_id = authResponse.orgId
      )
      val ws = MockWS {
        case (POST,url) => Action { implicit request => Ok(Json.toJson(introspectionResponse)) }
      }
      val client = GestaltSecurityClient(ws, HTTP, securityHostname, securityPort, NOT_USED, NOT_USED)
      val creds = GestaltBearerCredentials(token.toString)
      val request = FakeRequest("GET", "securedEndpoint", FakeHeaders(
        Seq(AUTHORIZATION -> s"token=${token.id}")
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(Some("root"),request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beSome(authResponse)
    }

    "fail on inactive remote token validation" in {
      val securityHostname = "security.local"
      val securityPort = 9455
      val url = s"http://${securityHostname}:${securityPort}/root/oauth/inspect"
      val token = OpaqueToken(UUID.randomUUID(), ACCESS_TOKEN)
      val NOT_USED = "nokey"
      val ws = MockWS {
        case (POST,url) => Action { implicit request => Ok(Json.toJson(INVALID_TOKEN))}
      }
      val client = GestaltSecurityClient(ws, HTTP, securityHostname, securityPort, NOT_USED, NOT_USED)
      val creds = GestaltBearerCredentials(token.toString)
      val request = FakeRequest("GET", "securedEndpoint", FakeHeaders(
        Seq(AUTHORIZATION -> creds.headerValue)
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(Some("root"),request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beNone
    }

    "fail on unparsable token" in {
      val securityHostname = "security.local"
      val securityPort = 9455
      val NOT_USED = "nokey"
      val ws = MockWS {
        case (_,_) => Action { BadRequest("shouldn't get this far") }
      }
      val client = GestaltSecurityClient(ws, HTTP, securityHostname, securityPort, NOT_USED, NOT_USED)
      val creds = GestaltBearerCredentials("not-a-valid-token")
      val request = FakeRequest("GET", "securedEndpoint", FakeHeaders(
        Seq(AUTHORIZATION -> creds.headerValue)
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(Some("root"),request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beNone
    }

  }

}
