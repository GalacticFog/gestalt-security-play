package com.galacticfog.gestalt.security.play.silhouette.utils

import com.mohiva.play.silhouette.api.SecuredSettings
import play.api.i18n.Lang
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The global configuration.
 */
trait DefaultSecurityPrompts extends SecuredSettings {
  this : com.galacticfog.gestalt.security.play.silhouette.utils.DefaultSecurityPrompts with play.api.GlobalSettings =>

  /**
   * Called when a user is not authenticated.
   *
   * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
   *
   * @param request The request header.
   * @param lang The currently selected language.
   * @return The result to send to the client.
   */
  override def onNotAuthenticated(request: RequestHeader, lang: Lang): Option[Future[Result]] = Some(Future{
    Unauthorized("Authentication required").withHeaders("WWW-Authenticate" -> "basic")
  })

}


