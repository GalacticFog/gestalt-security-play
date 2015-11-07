package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthProvider._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Request
import scala.concurrent.Future

class GestaltAuthProvider(appId: UUID, client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {
  override def id: String = ID

  override def gestaltAuth[B](request: Request[B], client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltBaseAuthProvider.getCredentials(request) match {
      case Some(creds) =>
        GestaltApp.authorizeUser(appId, GestaltBasicCredsToken(creds.identifier, creds.password))(client)
      case None =>
        Future.successful(None)
    }
  }
}

object GestaltAuthProvider {
  val ID = "gestalt-auth"
}
