package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api._

import com.mohiva.play.silhouette.api.util.Credentials
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class GestaltAuthResponseWithCreds(val gar: GestaltAuthResponse, val creds: Credentials) extends GestaltAuthResponse(gar.account, gar.groups, gar.rights) {
  def getUsername: String = creds.identifier
  def getPassword: String = creds.password
}

class GestaltFrameworkAuthProvider(client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {

  val usernameAndDomain = """(\w+)@([a-zA-Z0-9-]+)""".r

  override def id: String = GestaltFrameworkAuthProvider.ID

  override def gestaltAuth[B](request: Request[B], client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltBaseAuthProvider.getCredentials(request) match {
      case Some(creds) =>
        request match {
          case ocr: OrgContextRequest[B] =>
            // get org from request
            ocr.orgFQON match {
              case fqon if !fqon.trim.isEmpty =>
                GestaltOrg.authorizeFrameworkUser(fqon.trim, creds.identifier, creds.password)(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success,creds) }
                }
              case _ =>
                Future.successful(None)
            }
          case _ =>
            // try to get org from credentials
            creds.identifier match {
              case usernameAndDomain(username,domain) =>
                GestaltOrg.authorizeFrameworkUser(domain, creds.identifier, creds.password)(client) map {
                  _.map { success => new GestaltAuthResponseWithCreds(success,creds) }
                }
              case _ =>
                Future.successful(None)
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
