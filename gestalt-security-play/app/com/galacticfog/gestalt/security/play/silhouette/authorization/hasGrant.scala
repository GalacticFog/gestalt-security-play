package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthIdentity
import com.mohiva.play.silhouette.api.{Authenticator, Authorization}
import play.api.i18n.Messages
import play.api.mvc.Request
import scala.concurrent.Future

case class hasGrant[A <: Authenticator](grantName: String)
  extends Authorization[GestaltAuthIdentity, A] {
  
  override def isAuthorized[B]( identity: GestaltAuthIdentity, authenticator: A )
                              ( implicit request: Request[B] ): Future[Boolean] = {
    Future.successful( identity.rights.exists(_.grantName == grantName) )
  }

}
