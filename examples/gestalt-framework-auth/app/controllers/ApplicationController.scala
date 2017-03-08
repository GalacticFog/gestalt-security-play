package controllers

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.GestaltFrameworkSecurity
import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Controller, RequestHeader}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()( sec: GestaltFrameworkSecurity )
                                     ( implicit ec: ExecutionContext ) extends Controller {

  // simple auth, and say hello
  def hello() = sec.AuthAction() { securedRequest =>
    val account = securedRequest.identity.account
    Ok(s"hello, ${account.username}")
  }

  // auth against fqon according to some request header
  def inSitu() = sec.AuthAction({rh: RequestHeader => rh.headers.get("FQON")}) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id})")
  }

  // like above, but async+json
  def inSituJsonAsync() = sec.AuthAction({rh: RequestHeader => rh.headers.get("FQON")}).async(parse.json) {
    securedRequest => Future {
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id})")
    }
  }

  // pass fqon for authenticating org
  def fromArgs(fqon: String) = sec.AuthAction(Some(fqon)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated ${account.username} (${account.id}) on org ${fqon}")
  }

  // how about a UUID? we got that covered! this authenticates against /orgs/:orgId/auth
  def aUUID(orgId: UUID) = sec.AuthAction(Some(orgId)) {
    securedRequest =>
      val account = securedRequest.identity.account
      Ok(s"${account.id} authenticated with username ${account.username} on org ${orgId}")
  }

  // how about some authenticated methods with a credential-passthrough call to security?
  def createOrgPassthrough(parentOrgId: UUID) = sec.AuthAction(Some(parentOrgId)).async(parse.json) {
    securedRequest =>
      val account = securedRequest.identity.account
      val creds = securedRequest.identity.creds
      GestaltOrg.createSubOrg(parentOrgId = parentOrgId, GestaltOrgCreate("someNewOrgName"))(sec.securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Created(Json.obj(
          "newAccountId" -> newOrg.id,
          "createdBy" -> account.id
        ))
      }
  }

  def deleteOrgPassthrough(orgId: UUID) = sec.AuthAction(Some(orgId)).async(parse.json) {
    securedRequest =>
      val account = securedRequest.identity.account
      val creds = securedRequest.identity.creds
      GestaltOrg.deleteOrg(orgId)(sec.securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Ok(Json.obj(
          "deletedOrgId" -> orgId,
          "deletedBy" -> account.id
        ))
      }
  }

  def createAccountPassthrough(parentOrgId: UUID) = sec.AuthAction(Some(parentOrgId)).async(parse.json) {
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
      ))(sec.securityClient.withCreds(creds)) map {
        // do what you were going to do
        newOrg => Created(Json.obj(
          "newAccountId" -> newOrg.id,
          "createdBy" -> account.id,
          "authenticatedIn" -> securedRequest.identity.authenticatingOrgId
        ))
      }
  }

}
