package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._

import com.mohiva.play.silhouette.api.util.Credentials
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class GestaltAuthResponseWithCreds(override val account: GestaltAccount, override val groups: Seq[GestaltGroup], override val rights: Seq[GestaltRightGrant], override val orgId: UUID, val creds: Credentials) extends GestaltAuthResponse(account, groups, rights, orgId)

class GestaltFrameworkAuthProvider(client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {

  val usernameAndDomain = """(\w+)@([a-zA-Z0-9-.]+)""".r

  override def id: String = GestaltFrameworkAuthProvider.ID

  override def gestaltAuth[B](request: Request[B], client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {

    GestaltBaseAuthProvider.getCredentials(request) match {
      case Some(creds) =>
        request match {
          case OrgContextRequestUUID(Some(orgId),_) =>
            GestaltOrg.authorizeFrameworkUser(orgId = orgId, username = creds.identifier, password = creds.password)(client) map {
              _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
            }
          case OrgContextRequest(Some(fqon),_) =>
            GestaltOrg.authorizeFrameworkUser(orgFQON = fqon.trim, username = creds.identifier, password = creds.password)(client) map {
              _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
            }
          case _ =>
            creds.identifier match {
              case usernameAndDomain(username,domain) =>
                // got org from credentials; strip the org from the username
                GestaltOrg.authorizeFrameworkUser(domain, username, creds.password)(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
                }
              case _ =>
                // try without the org; valid API credentials will still succeed
                GestaltOrg.authorizeFrameworkUser(apiKey = creds.identifier, apiSecret = creds.password)(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success.account, success.groups, success.rights, success.orgId, creds) }
                }
            }
        }
      case None =>
        Future.successful(None)
    }
  }
}

object GestaltFrameworkAuthProvider {
  val ID = "gestalt-framework-auth"
}
