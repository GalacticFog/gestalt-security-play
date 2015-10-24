package com.galacticfog.gestalt.security.play.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class AccountServiceImpl extends IdentityService[AuthAccount] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthAccount]] = Future {
    loginInfo match {
      case glo: GestaltLoginInfo =>
        Some(AuthAccount(glo.authResponse.account, glo.authResponse.groups, glo.authResponse.rights))
      case _ => None
    }
  }
}

class AccountServiceImplWithCreds extends IdentityService[AuthAccountWithCreds] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthAccountWithCreds]] = Future {
    loginInfo match {
      case glo: GestaltLoginInfoWithCreds =>
        Some(AuthAccountWithCreds(glo.authResponse.account, glo.authResponse.groups, glo.authResponse.rights, glo.creds))
      case _ => None
    }
  }
}
