package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api.util.Credentials
import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthProvider._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ LoginInfo, RequestProvider }
import com.mohiva.play.silhouette.impl.exceptions.InvalidPasswordException
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Request, RequestHeader }

import scala.concurrent.Future

class GestaltLoginInfo(override val providerID: String, override val providerKey: String, val authResponse: GestaltAuthResponse) extends LoginInfo(providerID, providerKey) {

}

class GestaltAuthProvider(appId: String, client: GestaltSecurityClient) extends RequestProvider {
  override def authenticate[B](request: Request[B]): Future[Option[LoginInfo]] = {
    getCredentials(request) match {
      case Some(credentials) =>
        Logger.debug(s"attempting to authenticate REST user ${credentials.identifier}")
        val app = GestaltApp(appId = appId, appName = "", org = GestaltOrg("", ""))
        val auth: Future[Option[GestaltAuthResponse]] = app.authorizeUser(GestaltBasicCredsToken(credentials.identifier, credentials.password))(client)
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
      case None => Future.successful(None)
    }
  }

  override def id: String = ID

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


object GestaltAuthProvider {

  /**
   * The error messages.
   */
  val UnknownCredentials = "[Silhouette][%s] Could not find auth info for given credentials"
  val InvalidPassword = "[Silhouette][%s] Passwords does not match"

  /**
   * The provider constants.
   */
  val ID = "gestalt-auth"
}
