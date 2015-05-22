package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import scala.util.matching.Regex

case class matchesGrant(testGrantName: String) extends Authorization[AuthAccount] {

  val testSplit = splitAndValidate(testGrantName)

  def splitAndValidate(name: String): List[String] = {
    if (name.trim.isEmpty) throw new RuntimeException("grant name must be non-empty")
    val split = name.trim.split(":")
    val firstSuper: Int = split.indexOf("**")
    if (0 <= firstSuper && firstSuper != split.size-1) throw new RuntimeException("invalid matcher; super-wildcard must be in the right-most field")
    split.toList
  }

  def splitWildcardMatch(a: List[String], b: List[String]): Boolean = {
    (a,b) match {
      case ( "**" :: aTail, _ ) => true
      case ( _, "**" :: bTail ) => true
      case ( Nil, Nil ) => true
      case ( aHead :: aTail, bHead :: bTail ) if (aHead == "*" || bHead == "*" || aHead == bHead) => splitWildcardMatch(aTail,bTail)
      case _ => false
    }
  }

  override def isAuthorized(identity: AuthAccount)(implicit request: RequestHeader, lang: Lang): Boolean = {
    identity.rights.exists(r => splitWildcardMatch(testSplit, splitAndValidate(r.grantName)))
  }
}
