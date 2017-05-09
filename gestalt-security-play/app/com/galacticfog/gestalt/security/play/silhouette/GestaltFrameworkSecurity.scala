package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors.UnauthorizedAPIException
import com.galacticfog.gestalt.security.api.json.JsonImports.exceptionFormat
import com.google.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import play.api.Logger
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

  val secLogger = Logger("gestalt-security-play")

  implicit val securityClient: GestaltSecurityClient = environment.client

  def securityRealmOverride(orgFQON: String): Option[String] = environment.config.realm.map(
    _.stripSuffix("/") + s"/${orgFQON}/oauth/issue"
  )

  class GestaltFrameworkAuthActionBuilder(maybeGenFQON: Option[RequestHeader => Option[String]] = None)
    extends ActionBuilder[({ type R[B] = SecuredRequest[GestaltFrameworkSecurityEnvironment, B] })#R] {

    def invokeBlock[B](request: Request[B], block: SecuredRequest[GestaltFrameworkSecurityEnvironment,B] => Future[Result]) = {
      val ocr = OrgContextRequest(maybeGenFQON flatMap {_(request)}, request)
      sil.SecuredRequestHandler(ocr) { securedRequest =>
        secLogger.trace(s"req-${ocr.id}: request context: ${ocr}")
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) =>
          secLogger.trace(s"req-${request.id}: dispatching SecuredRequest to controller application block")
          block(sr)
        case HandlerResult(r, None) => Future {
          lazy val org = ocr.orgFQON getOrElse "root"
          lazy val defRealm = s"${securityClient.protocol}://${securityClient.hostname}:${securityClient.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride(org) getOrElse defRealm
          val challenge: String = "Bearer realm=\"" + realm + "\" error=\"invalid_token\""
          val resp = Unauthorized(Json.toJson(UnauthorizedAPIException("","Authentication required",""))).withHeaders(WWW_AUTHENTICATE -> challenge)
          secLogger.trace(s"req-${request.id}: authentication failed, returning ${resp}")
          resp
        }
      }
    }
  }

  class GestaltFrameworkAuthActionBuilderUUID(maybeGenOrgId: Option[RequestHeader => Option[UUID]] = None)
    extends ActionBuilder[({ type R[B] = SecuredRequest[GestaltFrameworkSecurityEnvironment, B] })#R] {

    def invokeBlock[B](request: Request[B], block: SecuredRequest[GestaltFrameworkSecurityEnvironment,B] => Future[Result]) = {
      val ocr = OrgContextRequestUUID(maybeGenOrgId flatMap {_(request)}, request)
      secLogger.trace(s"req-${request.id}: request context: ${ocr}")
      sil.SecuredRequestHandler(ocr) { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest)))
      }.flatMap {
        case HandlerResult(r, Some(sr)) =>
          secLogger.trace("req-${request.id}: dispatching SecuredRequest to controller application block")
          block(sr)
        case HandlerResult(r, None) => Future{
          lazy val org = ocr.orgId map {orgId => s"orgs/${orgId}"} getOrElse "root"
          lazy val defRealm = s"${securityClient.protocol}://${securityClient.hostname}:${securityClient.port}/${org}/oauth/issue"
          val realm: String = securityRealmOverride(org) getOrElse defRealm
          val challenge: String = "Bearer realm=\"" + realm + "\" error=\"invalid_token\""
          val resp = Unauthorized(Json.toJson(UnauthorizedAPIException("","Authentication required",""))).withHeaders(WWW_AUTHENTICATE -> challenge)
          secLogger.trace(s"req-${request.id}: authentication failed, returning ${resp}")
          resp
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

   class UserAwareActionBuilderFQON(gen: RequestHeader => Option[String]) extends UserAwareActionBuilder(Some(gen(_).map(Left(_))))

   class UserAwareActionBuilderUUID(gen: RequestHeader => Option[UUID]) extends UserAwareActionBuilder(Some(gen(_).map(Right(_))))

   class UserAwareActionBuilder(maybeOrgGen: Option[RequestHeader => Option[Either[String,UUID]]] = None) extends ActionBuilder[({ type R[B] = UserAwareRequest[GestaltFrameworkSecurityEnvironment, B] })#R] {
     override def invokeBlock[A](request: Request[A], block: (UserAwareRequest[GestaltFrameworkSecurityEnvironment,A]) => Future[Result]): Future[Result] = {
       val maybeOrg = maybeOrgGen flatMap {_(request)}
       val ocr = maybeOrg.fold[WrappedRequest[A]](
         OrgContextRequest(None, request)
       )(_.fold(
         {o => OrgContextRequest(Some(o), request)},
         {o => OrgContextRequestUUID(Some(o), request)}
       ))
       sil.UserAwareRequestHandler(ocr) { r =>
         block(r).map(r => HandlerResult(r))
       }.map(_.result)
     }
   }

   object UserAwareAction {
     def apply(): UserAwareActionBuilder = new UserAwareActionBuilder
     def apply(genFQON:  RequestHeader => Option[String]): UserAwareActionBuilderFQON = new UserAwareActionBuilderFQON(genFQON)
     def apply(genFQON:                => Option[String]): UserAwareActionBuilderFQON = new UserAwareActionBuilderFQON({_: RequestHeader => genFQON})
     def apply(genOrgId: RequestHeader => Option[UUID]):   UserAwareActionBuilderUUID = new UserAwareActionBuilderUUID(genOrgId)
     def apply(genOrgId:               => Option[UUID]):   UserAwareActionBuilderUUID = new UserAwareActionBuilderUUID({_: RequestHeader => genOrgId})
   }
 }

