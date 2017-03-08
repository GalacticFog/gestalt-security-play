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

case class matchesValue(grantName: String, grantValue: String)(matches: (String,String) => Boolean) extends Authorization[AuthAccount,DummyAuthenticator] {
  override def isAuthorized[B](identity: AuthAccount, authenticator: DummyAuthenticator)
                              ( implicit request: Request[B] ): Future[Boolean] = Future {
    identity.rights.exists( g =>
      g.grantName.equals(grantName) && g.grantValue.isDefined && matches(g.grantValue.get, grantValue)
    )
  }
}
