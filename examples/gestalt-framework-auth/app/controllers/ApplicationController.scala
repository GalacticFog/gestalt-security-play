package controllers

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.{AuthAccountWithCreds, GestaltFrameworkSecuredController, GestaltSecurityEnvironment}
import com.google.inject.Inject
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

object ApplicationController {
  type SecurityEnvironment = GestaltSecurityEnvironment[AuthAccountWithCreds,DummyAuthenticator]
}

class ApplicationController @Inject()( mApi: MessagesApi,
                                       env: ApplicationController.SecurityEnvironment )
                                     ( implicit ec: ExecutionContext )
  extends GestaltFrameworkSecuredController[DummyAuthenticator](mApi, env) {

  // simple auth, and say hello
  def hello() = GestaltFrameworkAuthAction() { securedRequest =>
    val account = securedRequest.identity.account
    Ok(s"hello, ${account.username}")
  }

  // auth against fqon according to some request header
  def inSitu() = GestaltFrameworkAuthAction({rh: RequestHeader => rh.headers.get("FQON")}) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id})")
  }

  // like above, but async+json
  def inSituJsonAsync() = GestaltFrameworkAuthAction({rh: RequestHeader => rh.headers.get("FQON")}).async(parse.json) {
    securedRequest => Future {
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id})")
    }
  }

  // pass fqon for authenticating org
  def fromArgs(fqon: String) = GestaltFrameworkAuthAction(Some(fqon)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id}) on org ${fqon}")
  }

  // how about a UUID? we got that covered! this authenticates against /orgs/:orgId/auth
  def aUUID(orgId: UUID) = GestaltFrameworkAuthAction(Some(orgId)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username} on org ${orgId}")
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
