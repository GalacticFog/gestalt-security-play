package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors.UnauthorizedAPIException
import com.galacticfog.gestalt.security.api.json.JsonImports.exceptionFormat
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.concurrent.Future

case class AuthAccountWithCreds(account: GestaltAccount, groups: Seq[ResourceLink], rights: Seq[GestaltRightGrant], creds: GestaltAPICredentials, authenticatingOrgId: UUID) extends Identity
case class OrgContextRequest[B](orgFQON: Option[String], request: Request[B]) extends WrappedRequest(request)
case class OrgContextRequestUUID[B](orgId: Option[UUID], request: Request[B]) extends WrappedRequest(request)

abstract class GestaltFrameworkSecuredController[A <: Authenticator]() extends Silhouette[AuthAccountWithCreds, A] {

  def getAuthenticator: AuthenticatorService[A]

  class GestaltFrameworkAuthActionBuilder(maybeGenFQON: Option[RequestHeader => Option[String]] = None) extends ActionBuilder[SecuredRequest] {
    def invokeBlock[B](request: Request[B], block: SecuredRequest[B] => Future[Result]) = {
      val ocr = OrgContextRequest(maybeGenFQON flatMap {_(request)}, request)
      SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Future{
          lazy val org = ocr.orgFQON getOrElse "root"
          lazy val defRealm = s"${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride getOrElse defRealm
          val challenge: String = "Bearer realm=\"" + realm + "\" error=\"invalid_token\""
          Unauthorized(Json.toJson(UnauthorizedAPIException("","Authentication required",""))).withHeaders(WWW_AUTHENTICATE -> challenge)
        }
      }
    }
  }

  class GestaltFrameworkAuthActionBuilderUUID(maybeGenOrgId: Option[RequestHeader => Option[UUID]] = None) extends ActionBuilder[SecuredRequest] {
    def invokeBlock[B](request: Request[B], block: SecuredRequest[B] => Future[Result]) = {
      val ocr = OrgContextRequestUUID(maybeGenOrgId flatMap {_(request)}, request)
      SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Future{
          lazy val org = ocr.orgId map {orgId => s"orgs/${orgId}"} getOrElse "root"
          lazy val defRealm = s"${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride getOrElse defRealm
          val challenge: String = "Bearer realm=\"" + realm + "\" error=\"invalid_token\""
          Unauthorized(Json.toJson(UnauthorizedAPIException("","Authentication required",""))).withHeaders(WWW_AUTHENTICATE -> challenge)
        }
      }
    }
  }

  object GestaltFrameworkAuthAction extends GestaltFrameworkAuthActionBuilder {
    def apply(genFQON: RequestHeader => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some(genFQON))
    def apply(genFQON: => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => genFQON}))
    def apply(genOrgId: RequestHeader => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some(genOrgId))
    def apply(genOrgId: => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => genOrgId}))
  }

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = None
  )

  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val securityConfig: GestaltSecurityConfig = try {
    Logger.info("attempting to determine GestaltSecurityConfig for framework authentication controller")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse GestaltSecurityConfig.getSecurityConfig
    c.flatMap( config =>
      if (config.mode == FRAMEWORK_SECURITY_MODE && config.isWellDefined) Some(config)
      else None
    ).getOrElse {
      Logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }

  val securityRealmOverride = scala.util.Properties.envOrNone("GESTALT_SECURITY_REALM")

  Logger.info(s"bound security in framework mode to ${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}")

  implicit val securityClient: GestaltSecurityClient = GestaltSecurityClient(securityConfig)
  val authProvider = new GestaltFrameworkAuthProvider(securityClient)

  // override for Silhouette
  val env = new Environment[AuthAccountWithCreds,A] {
    override def identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds()
    override def authenticatorService: AuthenticatorService[A] = getAuthenticator
    override def providers: Map[String, Provider] = Map(authProvider.id -> authProvider)
    override def eventBus: EventBus = EventBus()
  }

}
