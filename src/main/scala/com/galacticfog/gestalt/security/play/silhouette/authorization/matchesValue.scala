package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

case class matchesValue(grantName: String, grantValue: String)(matches: (String,String) => Boolean) extends Authorization[AuthAccount] {
  override def isAuthorized(identity: AuthAccount)(implicit request: RequestHeader, lang: Lang): Boolean = {
    identity.rights.exists( g =>
      g.grantName.equals(grantName) && g.grantValue.isDefined && matches(g.grantValue.get, grantValue)
    )
  }
}
