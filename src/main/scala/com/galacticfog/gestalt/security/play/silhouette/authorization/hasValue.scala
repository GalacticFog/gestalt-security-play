package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import play.api.i18n.Messages
import play.api.mvc.Request
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class hasValue(grantName: String, grantValue: String) extends Authorization[AuthAccount,DummyAuthenticator] {
  override def isAuthorized[B](identity: AuthAccount, authenticator: DummyAuthenticator)(
      implicit request: Request[B], messages: Messages): Future[Boolean] = Future {
    identity.rights.exists( g =>
      g.grantName == grantName && g.grantValue.isDefined && g.grantValue.get.equals(grantValue)
    )
  }
}
