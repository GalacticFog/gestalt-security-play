package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse}
import com.mohiva.play.silhouette.api.{LoginInfo, RequestProvider}

import play.api.Logger
import play.api.mvc.Request
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

class GestaltLoginInfo(override val providerID: String, override val providerKey: String, val authResponse: GestaltAuthResponse) extends LoginInfo(providerID, providerKey)

class GestaltLoginInfoWithCreds(override val providerID: String, override val providerKey: String, val authResponse: GestaltAuthResponseWithCreds, val creds: GestaltAPICredentials) extends LoginInfo(providerID, providerKey)

abstract class GestaltBaseAuthProvider extends RequestProvider {

  def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]]

  override def authenticate[B](request: Request[B]): Future[Option[LoginInfo]] = {
    val auth = gestaltAuthImpl(request)
    auth map { _.map { ar: GestaltAuthResponse =>
      ar match {
        case arc: GestaltAuthResponseWithCreds => new GestaltLoginInfoWithCreds(
          providerID = id,
          providerKey = arc.account.username,
          authResponse = arc,
          creds = arc.creds
        )
        case ar => new GestaltLoginInfo(
          providerID = id,
          providerKey = ar.account.username,
          authResponse = ar
        )
      }
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

