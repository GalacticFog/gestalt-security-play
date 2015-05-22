package com.galacticfog.gestalt.security.play.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait AccountService extends IdentityService[AuthAccount]

class AccountServiceImpl extends AccountService {
  override def retrieve(loginInfo: LoginInfo): Future[Option[AuthAccount]] = Future {
    loginInfo match {
      case glo: GestaltLoginInfo => Some(AuthAccount(glo.authResponse.account, glo.authResponse.rights))
      case _ => None
    }
  }
}
