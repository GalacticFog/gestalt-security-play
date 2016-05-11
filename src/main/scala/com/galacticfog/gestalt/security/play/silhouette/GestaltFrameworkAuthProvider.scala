package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import com.galacticfog.gestalt.security.api._

import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Failure, Success, Try}

class GestaltAuthResponseWithCreds(override val account: GestaltAccount, override val groups: Seq[GestaltGroup], override val rights: Seq[GestaltRightGrant], override val orgId: UUID, val creds: GestaltAPICredentials) extends GestaltAuthResponse(account, groups, rights, orgId)

class GestaltFrameworkAuthProvider(client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {

  val usernameAndDomain = """(\w+)@([a-zA-Z0-9-.]+)""".r

  override def id: String = GestaltFrameworkAuthProvider.ID

  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials match {
      case Some(creds: GestaltBearerCredentials) =>
        Logger.info("found Bearer credentials; will validate against gestalt-security")
        val fIntroResp = Try{UUID.fromString(creds.token)} match {
          case Failure(err) =>
            Logger.info("error parsing token ID: " + err.getMessage)
            Future.successful(INVALID_TOKEN)
          case Success(tokenId) =>
            val token = OpaqueToken(tokenId, ACCESS_TOKEN)
            request match {
              case OrgContextRequestUUID(Some(orgId),_) => GestaltOrg.validateToken(orgId = orgId, token = token)(client)
              case OrgContextRequest(Some(fqon),_) => GestaltOrg.validateToken(orgFQON = fqon, token = token)(client)
              case _ => GestaltOrg.validateToken(token = token)(client)
            }
        }
        fIntroResp map {
          _ match {
            case INVALID_TOKEN => None
            case valid: ValidTokenResponse => Some(new GestaltAuthResponseWithCreds(
              account = valid.gestalt_account,
              rights = valid.gestalt_rights,
              groups = valid.gestalt_groups,
              orgId = valid.gestalt_org_id,
              creds = creds
            ))
          }
        }
      case Some(creds: GestaltBasicCredentials) =>
        Logger.info("authentication via Basic credentials no longer supported")
        Future.successful(None)
      case None =>
        Logger.info("did not find credentials in request Authorization header")
        Future.successful(None)
    }
  }
}

object GestaltFrameworkAuthProvider {
  val ID = "gestalt-framework-auth"
}
