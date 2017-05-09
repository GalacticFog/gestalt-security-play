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

class GestaltAuthResponseWithCreds(override val account: GestaltAccount, override val groups: Seq[ResourceLink], override val rights: Seq[GestaltRightGrant], override val orgId: UUID, val creds: GestaltAPICredentials, override val extraData: Option[Map[String,String]]) extends GestaltAuthResponse(account, groups, rights, orgId, extraData)

class GestaltFrameworkAuthProvider(client: GestaltSecurityClient) extends GestaltBaseAuthProvider {

  val secLogger = Logger("gestalt-security-play")

  val usernameAndDomain = """(\w+)@([a-zA-Z0-9-.]+)""".r

  override def id: String = GestaltFrameworkAuthProvider.ID

  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    val extraData = Map(
      "gestalt-security-play-request-id" -> request.id.toString,
      "gestalt-security-play-request-uri" -> request.uri,
      "gestalt-security-play-request-time" -> System.currentTimeMillis().toString
    )
    request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials match {
      case Some(creds: GestaltBearerCredentials) =>
        val fIntroResp = Try{UUID.fromString(creds.token)} match {
          case Failure(err) =>
            secLogger.warn(s"req-${request.id}: error parsing Bearer token as UUID, will not attempt to authenticate: " + err.getMessage)
            Future.successful(INVALID_TOKEN)
          case Success(tokenId) =>
            secLogger.trace(s"req-${request.id}: found Bearer credentials; will validate against gestalt-security")
            val token = OpaqueToken(tokenId, ACCESS_TOKEN)
             request match {
              case OrgContextRequestUUID(Some(orgId),_) => GestaltToken.validateToken(orgId = orgId, token = token, extraData = extraData)(client)
              case OrgContextRequest(Some(fqon),_) => GestaltToken.validateToken(orgFQON = fqon, token = token, extraData = extraData)(client)
              case _ => GestaltToken.validateToken(token = token, extraData = extraData)(client)
            }
        }
        fIntroResp onComplete {
          case Success(resp) =>
            secLogger.trace(s"req-${request.id}: received auth response from gestalt-security: token.active == ${resp.active}")
          case Failure(ex) =>
            secLogger.error(s"req-${request.id}: error while validating Bearer token against gestalt-security", ex)
        }
        fIntroResp map {
          _ match {
            case INVALID_TOKEN => None
            case valid: ValidTokenResponse => Some(new GestaltAuthResponseWithCreds(
              account = valid.gestalt_account,
              rights = valid.gestalt_rights,
              groups = valid.gestalt_groups,
              orgId = valid.gestalt_org_id,
              creds = creds,
              extraData = valid.extra_data
            ))
          }
        }
      case Some(creds: GestaltBasicCredentials) =>
        secLogger.trace(s"req-${request.id}: found Basic credentials; will validate against gestalt-security")
        val authResponse = request match {
          case OrgContextRequestUUID(Some(orgId),_) => GestaltOrg.authorizeFrameworkUser(orgId,creds,extraData)(client)
          case OrgContextRequest(Some(fqon),_) => GestaltOrg.authorizeFrameworkUser(fqon,creds,extraData)(client)
          case _ => GestaltOrg.authorizeFrameworkUser(creds,extraData)(client)
        }
        authResponse onComplete {
          case Success(resp) =>
            secLogger.trace(s"req-${request.id}: received auth response from gestalt-security: credentials.valid == ${resp.isDefined}")
          case Failure(ex) =>
            secLogger.error(s"req-${request.id}: error while validating API credentials against gestalt-security", ex)
        }
        authResponse map {  _.map { ar =>
          new GestaltAuthResponseWithCreds(
            account = ar.account,
            rights = ar.rights,
            groups = ar.groups,
            orgId = ar.orgId,
            creds = creds,
            extraData = ar.extraData
          )
        } }
      case None =>
        secLogger.trace(s"req-${request.id}: did not find credentials in request Authorization header")
        Future.successful(None)
    }
  }
}

object GestaltFrameworkAuthProvider {
  val ID = "gestalt-framework-auth"
}
