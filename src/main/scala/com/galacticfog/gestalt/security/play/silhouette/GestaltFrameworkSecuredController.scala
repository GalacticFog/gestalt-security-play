package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util.Credentials
import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import scala.concurrent.Future

case class AuthAccountWithCreds(account: GestaltAccount, groups: Seq[GestaltGroup], rights: Seq[GestaltRightGrant], creds: Credentials) extends Identity
case class OrgContextRequest[B](orgFQON: String, request: Request[B]) extends WrappedRequest(request)

abstract class GestaltFrameworkSecuredController[A <: Authenticator](val meta: Option[Gestalt] = None) extends Silhouette[AuthAccountWithCreds, A] {

  def getAuthenticator: AuthenticatorService[A]

  class GestaltFrameworkAuthActionBuilder(maybeGenFQON: Option[RequestHeader => String] = None) extends ActionBuilder[SecuredRequest] {
    def invokeBlock[B](request: Request[B], block: SecuredRequest[B] => Future[Result]) = {
      val ocr = maybeGenFQON match {
        case Some(genFQON) =>
          OrgContextRequest(genFQON(request), request)
        case None =>
          OrgContextRequest("", request)
      }
      SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Future.successful(Unauthorized)
      }
    }
  }

  object GestaltFrameworkAuthAction extends GestaltFrameworkAuthActionBuilder {
    def apply(genFQON: RequestHeader => String) = new GestaltFrameworkAuthActionBuilder(Some(genFQON))
    def apply(genFQON: => String) = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => genFQON}))
  }

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = None,
    apiSecret = None,
    appId = None
  )

  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val securityConfig: GestaltSecurityConfig = try {
    Logger.info("attempting to determine GestaltSecurityConfig for framework authentication controller")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse GestaltSecurityConfig.getSecurityConfig(meta)
    c.flatMap( config =>
      if (config.mode == FRAMEWORK_SECURITY_MODE && config.isWellDefined) Some(config)
      else None
    ).getOrElse {
      Logger.warn("Could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }

  Logger.info(s"bound security in framework mode to ${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}")

  val securityClient: GestaltSecurityClient = GestaltSecurityClient(securityConfig)
  val authProvider = new GestaltFrameworkAuthProvider(securityClient)

  // override for Silhouette
  val env = new Environment[AuthAccountWithCreds,A] {
    override def identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds()
    override def authenticatorService: AuthenticatorService[A] = getAuthenticator
    override def providers: Map[String, Provider] = Map(authProvider.id -> authProvider)
    override def eventBus: EventBus = EventBus()
  }

}
