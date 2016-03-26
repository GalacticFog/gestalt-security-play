package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthProvider._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Request
import scala.concurrent.Future

class GestaltAuthProvider(appId: UUID, client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {
  override def id: String = ID

  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    GestaltBaseAuthProvider.getCredentials(request) match {
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

object GestaltAuthProvider {
  val ID = "gestalt-auth"
}
