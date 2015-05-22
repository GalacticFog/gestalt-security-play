package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import scala.util.matching.Regex

case class matchesGrant(grantName: String) extends Authorization[AuthAccount] {
  override def isAuthorized(identity: AuthAccount)(implicit request: RequestHeader, lang: Lang): Boolean = {
    identity.rights.exists(r => {
      val test = grantName.split(":")
      val name = r.grantName.split(":")
      if (test.size != name.size) false
      else {
        !test.zip(name).exists( z => z._1 != "*" && z._1 != z._2 )
      }
    })
  }
}
