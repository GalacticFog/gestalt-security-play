package com.galacticfog.gestalt.security.play.silhouette


import java.util.UUID

import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import mockws.MockWS
import org.joda.time.DateTime
import org.specs2.execute
import org.specs2.matcher.ValueCheck
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test._
import play.mvc.Http.HeaderNames
import scala.concurrent.Future
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.global

@RunWith(classOf[JUnitRunner])
class GestaltPlaySpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  val testConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "test.host.com",
    port = 1234,
    apiKey = "someKey",
    apiSecret = "someSecret",
    appId = Some(UUID.randomUUID)
  )

  "GestaltFrameworkSecuredController" should {

    class TestController(config: Option[GestaltSecurityConfig] = None) extends GestaltFrameworkSecuredController[DummyAuthenticator] {

      override def getSecurityConfig: Option[GestaltSecurityConfig] = config

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
      def aUUID(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)).async {
        securedRequest => Future{Ok(securedRequest.identity.account.id.toString + " authenticated with username " + securedRequest.identity.creds)}
      }

      def hello() = GestaltFrameworkAuthAction(Option.empty[String]) { securedRequest =>
        Ok("hello")
      }

      // how about some authenticated methods with a credential-passthrough call to security?
      def createOrgPassthrough(parentOrgId: UUID) = GestaltFrameworkAuthAction(Some(parentOrgId)).async(parse.json) {
        securedRequest =>
          val account = securedRequest.identity.account
          val creds = securedRequest.identity.creds
          GestaltOrg.createSubOrg(parentOrgId = parentOrgId, GestaltOrgCreate("someNewOrgName"))(securityClient.withCreds(creds)) map {
            // do what you were going to do
            newOrg => Created(Json.obj(
              "newAccountId" -> newOrg.id,
              "createdBy" -> account.id
            ))
          }
      }

      def deleteOrgPassthrough(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)).async(parse.json) {
        securedRequest =>
          val account = securedRequest.identity.account
          val creds = securedRequest.identity.creds
          GestaltOrg.deleteOrg(orgId)(securityClient.withCreds(creds)) map {
              // do what you were going to do
            newOrg => Ok(Json.obj(
              "deletedOrgId" -> orgId,
              "deletedBy" -> account.id
            ))
          }
      }

      def createAccountPassthrough(parentOrgId: UUID) = GestaltFrameworkAuthAction(Some(parentOrgId)).async(parse.json) {
        securedRequest =>
          val account = securedRequest.identity.account
          val someExistingGroupId = UUID.randomUUID()
          val creds = securedRequest.identity.creds
          GestaltOrg.createAccount(orgId = parentOrgId, GestaltAccountCreateWithRights(
            username = "bsmith",
            firstName = "bob",
            lastName = "smith",
            email = Some("bsmith@myorg"),
            phoneNumber = Some("505-867-5309"),
            credential = GestaltPasswordCredential("bob's password"),
            groups = Some(Seq(someExistingGroupId)),
            rights = Some(Seq(GestaltGrantCreate("freedom")))
          ))(securityClient.withCreds(creds)) map {
              // do what you were going to do
            newOrg => Created(Json.obj(
              "newAccountId" -> newOrg.id,
              "createdBy" -> account.id,
              "authenticatedIn" -> securedRequest.identity.authenticatingOrgId
            ))
          }
      }

    }

    // requires WithApplication to create wsclient
    "allow easy specification of config via override" in new WithApplication {
      val controller = new TestController(Some(testConfig))
      controller.securityConfig.protocol   must_== testConfig.protocol
      controller.securityConfig.hostname   must_== testConfig.hostname
      controller.securityConfig.port       must_== testConfig.port
      controller.securityConfig.apiKey     must_== testConfig.apiKey
      controller.securityConfig.apiSecret  must_== testConfig.apiSecret
      controller.securityConfig.appId      must_== testConfig.appId
    }

    "return WWW-Authenticate header on 401 (root)" in new WithApplication {
      val controller = new TestController()
      val realm = s"${controller.securityClient.protocol}://${controller.securityClient.hostname}:${controller.securityClient.port}/root/oauth/issue"
      val result = await(controller.hello().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return WWW-Authenticate header on 401 (FQON)" in new WithApplication {
      val controller = new TestController()
      val fqon = "galacticfog"
      val realm = s"${controller.securityClient.protocol}://${controller.securityClient.hostname}:${controller.securityClient.port}/$fqon/oauth/issue"
      val result = await(controller.fromArgs(fqon).apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    val fakeApplication = FakeApplication()

    "return WWW-Authenticate header on 401 (realm-override)" in new WithApplication() {
      val controller = new TestController() {
        override def securityRealmOverride(org: String): Option[String] = Some("https://realm.override:9455")
      }
      val realm = "https://realm.override:9455"
      val result = await(controller.hello().apply(FakeRequest()))
      result.header.status must_== 401
      val w = result.header.headers.get(HeaderNames.WWW_AUTHENTICATE)
      w must beSome
      w.get.split(" ").headOption must beSome("Bearer")
      w.get.split(" ").toSeq must containAllOf(Seq("realm=\"" + realm + "\"", "error=\"invalid_token\""))
    }

    "return WWW-Authenticate header on 401 (UUID)" in new WithApplication {
      val controller = new TestController()
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

    import com.galacticfog.gestalt.security.api.json.JsonImports.authFormat
    import com.galacticfog.gestalt.security.api.json.JsonImports.tokenIntrospectionResponse

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
        Seq(AUTHORIZATION -> Seq(creds.headerValue))
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
        Seq(AUTHORIZATION -> Seq(creds.headerValue))
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
        Seq(AUTHORIZATION -> Seq(s"token=${token.id}"))
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
        Seq(AUTHORIZATION -> Seq(creds.headerValue))
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
        Seq(AUTHORIZATION -> Seq(creds.headerValue))
      ), AnyContentAsEmpty)
      val ocRequest = OrgContextRequest(Some("root"),request)
      val frameworkProvider = new GestaltFrameworkAuthProvider(client)
      val resp = await(frameworkProvider.gestaltAuthImpl(ocRequest))
      resp must beNone
    }

  }

}
