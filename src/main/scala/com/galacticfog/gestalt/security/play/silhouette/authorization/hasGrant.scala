package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

case class hasGrant(grantName: String) extends Authorization[AuthAccount] {
  override def isAuthorized(identity: AuthAccount)(implicit request: RequestHeader, lang: Lang): Boolean = {
    identity.rights.exists(_.grantName == grantName)
  }
}
