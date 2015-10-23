package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api.services.{IdentityService, AuthenticatorService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.{Result, WrappedRequest, Request}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

sealed trait AuthAccount extends Identity {
  def account: GestaltAccount
  def groups: Seq[GestaltGroup]
  def rights: Seq[GestaltRightGrant]
}
case class AuthAccountSimple(override val account: GestaltAccount, override val groups: Seq[GestaltGroup], override val rights: Seq[GestaltRightGrant]) extends AuthAccount
case class AuthAccountWithCreds(override val account: GestaltAccount, override val groups: Seq[GestaltGroup], override val rights: Seq[GestaltRightGrant], creds: Credentials) extends AuthAccount

case class OrgContextRequest[B](orgFQON: String, request: Request[B]) extends WrappedRequest(request)

class GestaltSecuredController(val meta: Option[Gestalt]) extends Silhouette[AuthAccount, DummyAuthenticator] {

  def this() = this(meta = None)

  trait GestaltFrameworkAuthRequestBuilder {
    final def apply(genFQON: => String)(block: SecuredRequest[play.api.mvc.AnyContent] => play.api.mvc.Result)(implicit request: Request[play.api.mvc.AnyContent]) = {
      val ocr: OrgContextRequest[play.api.mvc.AnyContent] = OrgContextRequest(genFQON, request)
      SecuredRequestHandler { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }(ocr).map {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Unauthorized
      }
    }

    final def apply[B](request: Request[B])(genFQON: => String)(block: SecuredRequest[B] => play.api.mvc.Result) = {
      val ocr = OrgContextRequest(genFQON, request)
      SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.map {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Unauthorized
      }
    }
  }

  // TODO: we can augment this object in the manner of play.api.mvc.Action to make it easier to call
  /*
      def home() = GestaltFrameworkAuthRequest.async {} {}
   */
  object GestaltFrameworkAuthRequest extends GestaltFrameworkAuthRequestBuilder

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = None,
    apiSecret = None,
    appId = None)

  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val securityConfig: GestaltSecurityConfig = try {
    Logger.info("creating GestaltSecurityConfig")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse GestaltSecurityConfig.getSecurityConfig(meta)
    c.getOrElse {
      Logger.warn("Could not determine GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }

  val securityClient: GestaltSecurityClient = GestaltSecurityClient(securityConfig)
  val authProvider = securityConfig.mode match {
    case DELEGATED_SECURITY_MODE =>
      Logger.info(s"bound security in delegated mode to ${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}, apiKey: ${securityConfig.apiKey}, appId: ${securityConfig.appId}")
      new GestaltAuthProvider(securityConfig.appId.getOrElse(""), securityClient)
    case FRAMEWORK_SECURITY_MODE =>
      Logger.info(s"bound security in framework mode to ${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}")
      new GestaltFrameworkAuthProvider(securityClient)
  }

  // override for Silhouette
  val env = new Environment[AuthAccount,DummyAuthenticator] {
    override def identityService: IdentityService[AuthAccount] = new AccountServiceImpl()
    override def authenticatorService: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService()
    override def providers: Map[String, Provider] = Map(authProvider.id -> authProvider)
    override def eventBus: EventBus = EventBus()
  }

}

