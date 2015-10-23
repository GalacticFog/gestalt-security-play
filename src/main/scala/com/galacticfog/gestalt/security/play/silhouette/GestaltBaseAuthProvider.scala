package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltAuthResponse, GestaltSecurityClient}
import com.mohiva.play.silhouette.api.{LoginInfo, RequestProvider}
import com.mohiva.play.silhouette.api.util.{Base64, Credentials}
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.{Request, RequestHeader}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

class GestaltLoginInfo(override val providerID: String, override val providerKey: String, val authResponse: GestaltAuthResponse) extends LoginInfo(providerID, providerKey)

abstract class GestaltBaseAuthProvider(client: GestaltSecurityClient) extends RequestProvider {

  def gestaltAuth[B](request: Request[B], client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]]

  override def authenticate[B](request: Request[B]): Future[Option[LoginInfo]] = {
    val auth = gestaltAuth(request,client)
    auth map { _.map {
      ar => new GestaltLoginInfo(
        providerID = id,
        providerKey = ar.account.username,
        authResponse = ar
      )
    } } recover {
      case ce: java.net.ConnectException =>
        Logger.warn("ConnectException while trying to authenticate: " + ce.getMessage())
        None
      case t: Throwable =>
        Logger.warn("Caught exception while trying to authenticate",t)
        None
    }
  }

}

object GestaltBaseAuthProvider {

  /**
   * Decodes the credentials.
   *
   * @param request Contains the colon-separated name-value pairs in clear-text string format
   * @return The users credentials as plaintext
   */
  def getCredentials(request: RequestHeader): Option[Credentials] = {
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(header) => Base64.decode(header.replace("Basic ", "")).split(":") match {
        case credentials if credentials.length == 2 => Some(Credentials(credentials(0), credentials(1)))
        case _ => None
      }
      case None => None
    }
  }

}

