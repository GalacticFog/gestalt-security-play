package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._

import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class GestaltAuthResponseWithCreds(override val account: GestaltAccount, override val groups: Seq[GestaltGroup], override val rights: Seq[GestaltRightGrant], override val orgId: UUID, val creds: GestaltAPICredentials) extends GestaltAuthResponse(account, groups, rights, orgId)

class GestaltFrameworkAuthProvider(client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {

  val usernameAndDomain = """(\w+)@([a-zA-Z0-9-.]+)""".r

  override def id: String = GestaltFrameworkAuthProvider.ID

  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {

    request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials match {
      case Some(creds: GestaltBearerCredentials) =>
        Logger.info("found Bearer creds; cannot use these yet")
        Future.successful(None)
      case Some(creds: GestaltBasicCredentials) =>
        Logger.info("found Basic creds")
        request match {
          case OrgContextRequestUUID(Some(orgId),_) =>
            GestaltOrg.authorizeFrameworkUser(orgId = orgId, creds)(client) map {
              _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
            }
          case OrgContextRequest(Some(fqon),_) =>
            GestaltOrg.authorizeFrameworkUser(orgFQON = fqon.trim, creds)(client) map {
              _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
            }
          case _ =>
            creds.username match {
              case usernameAndDomain(username,domain) =>
                // got org from credentials; strip the org from the username
                GestaltOrg.authorizeFrameworkUser(domain, creds.copy(username = username))(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
                }
              case _ =>
                // try without the org; valid API credentials will still succeed
                GestaltOrg.authorizeFrameworkUser(creds)(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
                }
            }
        }
      case None =>
        Logger.info("did not find credentials in request Authorization header")
        Future.successful(None)
    }
  }
}

object GestaltFrameworkAuthProvider {
  val ID = "gestalt-framework-auth"
}
