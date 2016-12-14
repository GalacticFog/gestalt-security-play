package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import play.api.mvc.Request
import scala.concurrent.ExecutionContext.Implicits.global


case class hasGrant(grantName: String) extends Authorization[AuthAccount, DummyAuthenticator] {
  
  override def isAuthorized[B](identity: AuthAccount, authenticator: DummyAuthenticator)(
      implicit request: Request[B], messages: Messages): Future[Boolean] = {
    Future( identity.rights.exists(_.grantName == grantName) )
  }
  
}


//type [B](
//    identity: com.galacticfog.gestalt.security.play.silhouette.AuthAccount, 
//    authenticator: com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator)
//(implicit request: play.api.mvc.Request[B], 
//    messages: play.api.i18n.Messages) scala.concurrent.Future[Boolean]