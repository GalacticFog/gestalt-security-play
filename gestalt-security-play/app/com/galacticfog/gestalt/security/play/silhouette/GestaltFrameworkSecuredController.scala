package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID
import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors.UnauthorizedAPIException
import com.galacticfog.gestalt.security.api.json.JsonImports.exceptionFormat
import com.mohiva.play.silhouette.api._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

case class OrgContextRequest[B](orgFQON: Option[String], request: Request[B]) extends WrappedRequest(request)
case class OrgContextRequestUUID[B](orgId: Option[UUID], request: Request[B]) extends WrappedRequest(request)

abstract class GestaltFrameworkSecuredController[A <: Authenticator]( mAPI: MessagesApi,
                                                                      environment: GestaltSecurityEnvironment[AuthAccountWithCreds, A] )
  extends Silhouette[AuthAccountWithCreds, A] {

  implicit val securityClient: GestaltSecurityClient = environment.client

  override val messagesApi: MessagesApi = mAPI

  override val env: Environment[AuthAccountWithCreds, A] = environment

  def securityRealmOverride(orgFQON: String): Option[String] = environment.config.realm.map(
    _.stripSuffix("/") + s"/${orgFQON}/oauth/issue"
  )

  class GestaltFrameworkAuthActionBuilder(maybeGenFQON: Option[RequestHeader => Option[String]] = None) extends ActionBuilder[SecuredRequest] {
    def invokeBlock[B](request: Request[B], block: SecuredRequest[B] => Future[Result]) = {
      val ocr = OrgContextRequest(maybeGenFQON flatMap {_(request)}, request)
      SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) => block(sr)
        case HandlerResult(r, None) => Future{
          lazy val org = ocr.orgFQON getOrElse "root"
          lazy val defRealm = s"${securityClient.protocol}://${securityClient.hostname}:${securityClient.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride(org) getOrElse defRealm
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
          lazy val defRealm = s"${securityClient.protocol}://${securityClient.hostname}:${securityClient.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride(org) getOrElse defRealm
          val challenge: String = "Bearer realm=\"" + realm + "\" error=\"invalid_token\""
          Unauthorized(Json.toJson(UnauthorizedAPIException("","Authentication required",""))).withHeaders(WWW_AUTHENTICATE -> challenge)
        }
      }
    }
  }

  object GestaltFrameworkAuthAction extends GestaltFrameworkAuthActionBuilder {
    def apply(): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(None)
    def apply(genFQON: RequestHeader => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some(genFQON))
    def apply(genFQON: => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => genFQON}))
    def apply(genOrgId: RequestHeader => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some(genOrgId))
    def apply(genOrgId: => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => genOrgId}))
  }

}

