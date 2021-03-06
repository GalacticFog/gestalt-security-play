package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.json.JsonImports._
import com.galacticfog.gestalt.security.play.silhouette.modules.GestaltSecurityModule
import com.google.inject.{AbstractModule, Inject, Provides}
import com.mohiva.play.silhouette.api.{Silhouette, SilhouetteProvider}
import mockws.MockWS
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Application
import play.api.http.HeaderNames.WWW_AUTHENTICATE
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc.{Action, _}
import play.api.test.Helpers._
import play.api.test._

class SecurityConfigOverrideModule(config: GestaltSecurityConfig) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Silhouette[GestaltFrameworkSecurityEnvironment]].to[SilhouetteProvider[GestaltFrameworkSecurityEnvironment]]
  }

  @Provides
  def getFrameworkEnv(wsclient: WSClient): GestaltFrameworkSecurityEnvironment = new OverrideGestaltFrameworkSecurityEnvironment(wsclient, config)

  @Provides
  def getDelegatedEnv = new GestaltDelegatedSecurityEnvironment {
    override def client: GestaltSecurityClient = ???
    override def config: GestaltSecurityConfig = ???
  }
}

class OverrideGestaltFrameworkSecurityEnvironment(wsclient: WSClient, overrideConfig: GestaltSecurityConfig) extends GestaltFrameworkSecurityEnvironment {
  override def config = overrideConfig
  override def client = new GestaltSecurityClient(
    client = wsclient,
    protocol = overrideConfig.protocol,
    hostname = overrideConfig.hostname,
    port = overrideConfig.port,
    creds = GestaltBasicCredentials(overrideConfig.apiKey, overrideConfig.apiSecret)
  )
}

class TestSecuredController @Inject()( gestaltSecurity: GestaltFrameworkSecurity ) {
  val security = gestaltSecurity

  def helloAuthUser() = security.AuthAction() { securedRequest =>
    val account = securedRequest.identity.account
    Ok(s"hello, ${account.username}")
  }

  // pass fqon for authenticating org in an argument, presumably from route
  // this authenticates against /:fqon/auth
  def authOrgFromArg(fqon: String) = security.AuthAction(Some(fqon)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id}) on org ${fqon}")
  }

  // how about like above, but with a UUID? we got that covered! this authenticates against /orgs/:orgId/auth
  def authOrgFromArg(orgId: UUID) = security.AuthAction(Some(orgId)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username} on org ${orgId}")
  }
}

@RunWith(classOf[JUnitRunner])
class GestaltSecurityPlaySpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  implicit def envFromClient(_client: GestaltSecurityClient) = new GestaltSecurityEnvironment[AuthAccountWithCreds] {
    override def client: GestaltSecurityClient = _client
    override def config: GestaltSecurityConfig = ???
  }

  val testConfigOverride = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "test.host.com",
    port = 1234,
    apiKey = "someKey",
    apiSecret = "someSecret",
    appId = None,
    realm = None
  )

  def defaultTestApp(config: Option[GestaltSecurityConfig] = None): Application =
    new GuiceApplicationBuilder()
      .bindings(
        new SecurityConfigOverrideModule(config getOrElse testConfigOverride),
        new GestaltSecurityModule
      )
      .build

  abstract class SecurityPlayTestApp(a: Application = defaultTestApp(None)) extends WithApplication(a)

  "GestaltFrameworkSecuredController" should {

    // requires WithApplication to create wsclient
    "allow easy specification of config via constructor injection" in new SecurityPlayTestApp {
      val controller = app.injector.instanceOf[TestSecuredController]
      controller.security.securityClient.protocol   must_== testConfigOverride.protocol
      controller.security.securityClient.hostname   must_== testConfigOverride.hostname
      controller.security.securityClient.port       must_== testConfigOverride.port
      controller.security.securityClient.creds.asInstanceOf[GestaltBasicCredentials].username  must_== testConfigOverride.apiKey
      controller.security.securityClient.creds.asInstanceOf[GestaltBasicCredentials].password  must_== testConfigOverride.apiSecret
    }

    "return proper WWW-Authenticate header on 401 (root)" in new SecurityPlayTestApp {
      val controller = app.injector.instanceOf[TestSecuredController]
      val realm = s"${controller.security.securityClient.protocol}://${controller.security.securityClient.hostname}:${controller.security.securityClient.port}/root/oauth/issue"
      val result = await(controller.helloAuthUser().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return proper WWW-Authenticate header on 401 (FQON)" in new SecurityPlayTestApp {
      val controller = app.injector.instanceOf[TestSecuredController]
      val fqon = "galacticfog"
      val realm = s"${controller.security.securityClient.protocol}://${controller.security.securityClient.hostname}:${controller.security.securityClient.port}/$fqon/oauth/issue"
      val result = await(controller.authOrgFromArg(fqon).apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return proper WWW-Authenticate header on 401 (UUID)" in new SecurityPlayTestApp {
      val controller = app.injector.instanceOf[TestSecuredController]
      val orgId = UUID.randomUUID()
      val realm = s"${controller.security.securityClient.protocol}://${controller.security.securityClient.hostname}:${controller.security.securityClient.port}/orgs/$orgId/oauth/issue"
      val result = await(controller.authOrgFromArg(orgId).apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    lazy val realm = "https://realm.override:9455"

    "return proper WWW-Authenticate header on 401 (realm-override)" in new SecurityPlayTestApp(
      defaultTestApp(Some(testConfigOverride.copy(
        realm = Some(realm)
      )))
    ) {
      val controller = app.injector.instanceOf[TestSecuredController]
      val result = await(controller.helloAuthUser().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq(
        s"""realm="${realm}/root/oauth/issue"""",
        """error="invalid_token""""
      ))
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
        orgId = UUID.randomUUID(),
        extraData = None
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
        orgId = UUID.randomUUID(),
        extraData = None
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
        gestalt_org_id = authResponse.orgId,
        extra_data = None
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
        orgId = UUID.randomUUID(),
        extraData = None
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
        gestalt_org_id = authResponse.orgId,
        extra_data = None
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
