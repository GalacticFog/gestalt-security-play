package com.galacticfog.gestalt.security.play.silhouette.authorization

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import scala.util.matching.Regex

import play.api.i18n.Messages
import play.api.mvc.Request
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


case class matchesGrant(testGrantName: String) extends Authorization[AuthAccount,DummyAuthenticator] {
  import matchesGrant._

  val testSplit = splitAndValidate(testGrantName)

  def checkAuthorization(identity: AuthAccount): Boolean = {
    identity.rights.exists(r => splitWildcardMatch(testSplit, splitAndValidate(r.grantName)))
  }

  override def isAuthorized[B](identity: AuthAccount, authenticator: DummyAuthenticator)(
      implicit request: Request[B], messages: Messages): Future[Boolean] = Future {
    checkAuthorization(identity)
  }
}

case object matchesGrant {
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
}
