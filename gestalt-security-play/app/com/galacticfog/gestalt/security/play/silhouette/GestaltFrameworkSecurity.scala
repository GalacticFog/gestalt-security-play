package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors.UnauthorizedAPIException
import com.galacticfog.gestalt.security.api.json.JsonImports.exceptionFormat
import com.google.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.HeaderNames._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

case class OrgContextRequest[B](orgFQON: Option[String], request: Request[B]) extends WrappedRequest(request)
case class OrgContextRequestUUID[B](orgId: Option[UUID], request: Request[B]) extends WrappedRequest(request)

class GestaltFrameworkSecurity @Inject() ( environment: GestaltFrameworkSecurityEnvironment,
                                           sil: Silhouette[GestaltFrameworkSecurityEnvironment] )
 {

  implicit val securityClient: GestaltSecurityClient = environment.client

  def securityRealmOverride(orgFQON: String): Option[String] = environment.config.realm.map(
    _.stripSuffix("/") + s"/${orgFQON}/oauth/issue"
  )

  class GestaltFrameworkAuthActionBuilder(maybeGenFQON: Option[RequestHeader => Option[String]] = None)
    extends ActionBuilder[({ type R[B] = SecuredRequest[GestaltFrameworkSecurityEnvironment, B] })#R] {

    def invokeBlock[B](request: Request[B], block: SecuredRequest[GestaltFrameworkSecurityEnvironment,B] => Future[Result]) = {
      val ocr = OrgContextRequest(maybeGenFQON flatMap {_(request)}, request)
      sil.SecuredRequestHandler(ocr) { securedRequest =>
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

  class GestaltFrameworkAuthActionBuilderUUID(maybeGenOrgId: Option[RequestHeader => Option[UUID]] = None)
    extends ActionBuilder[({ type R[B] = SecuredRequest[GestaltFrameworkSecurityEnvironment, B] })#R] {

    def invokeBlock[B](request: Request[B], block: SecuredRequest[GestaltFrameworkSecurityEnvironment,B] => Future[Result]) = {
      val ocr = OrgContextRequestUUID(maybeGenOrgId flatMap {_(request)}, request)
      sil.SecuredRequestHandler(ocr) { securedRequest =>
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

  object AuthAction extends GestaltFrameworkAuthActionBuilder {
    def apply(): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(None)
    def apply(genFQON: RequestHeader => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some(genFQON))
    def apply(genFQON: => Option[String]): GestaltFrameworkAuthActionBuilder = new GestaltFrameworkAuthActionBuilder(Some({rh: RequestHeader => genFQON}))
    def apply(genOrgId: RequestHeader => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some(genOrgId))
    def apply(genOrgId: => Option[UUID]): GestaltFrameworkAuthActionBuilderUUID = new GestaltFrameworkAuthActionBuilderUUID(Some({rh: RequestHeader => genOrgId}))
  }

}

