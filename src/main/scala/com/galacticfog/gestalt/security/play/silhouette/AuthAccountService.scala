package com.galacticfog.gestalt.security.play.silhouette

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import com.google.inject.{Inject, Singleton}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

class AccountServiceImpl()(implicit ec: ExecutionContext) extends IdentityService[AuthAccount] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthAccount]] = Future {
    loginInfo match {
      case glo: GestaltLoginInfo =>
        Some(AuthAccount(glo.authResponse.account, glo.authResponse.groups, glo.authResponse.rights))
      case _ =>
        Logger.warn("AccountServiceImpl expects GestaltLogInfo... this suggests a bug or configuration error")
        None
    }
  }
}

class AccountServiceImplWithCreds()(implicit ec: ExecutionContext) extends IdentityService[AuthAccountWithCreds] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthAccountWithCreds]] = Future {
    loginInfo match {
      case glo: GestaltLoginInfoWithCreds =>
        Some(AuthAccountWithCreds(glo.authResponse.account, glo.authResponse.groups, glo.authResponse.rights, glo.creds, glo.authResponse.orgId))
      case _ =>
        Logger.warn("AccountServiceImplWithCreds expects GestaltLogInfoWithCreds... this suggests a bug or configuration error")
        None
    }
  }
}
