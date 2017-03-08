package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.GestaltDelegatedAuthProvider._
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Request

import scala.concurrent.Future

class GestaltDelegatedAuthProvider(appId: UUID, client: GestaltSecurityClient) extends GestaltBaseAuthProvider {
  override def id: String = ID

  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials match {
      case Some(creds: GestaltBasicCredentials) =>
        GestaltApp.authorizeUser(appId, GestaltBasicCredsToken(creds.username, creds.password))(client)
      case Some(creds: GestaltBearerCredentials) =>
        Logger.warn("token authentication against applications not supported yet")
        Future.successful(None)
      case None =>
        Future.successful(None)
    }
  }
}

object GestaltDelegatedAuthProvider {
  val ID = "gestalt-auth"
}
