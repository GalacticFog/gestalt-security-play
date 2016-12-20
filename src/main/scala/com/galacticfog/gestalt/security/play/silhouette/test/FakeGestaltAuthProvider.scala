package com.galacticfog.gestalt.security.play.silhouette.test

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse}
import com.galacticfog.gestalt.security.play.silhouette.{GestaltAuthResponseWithCreds, GestaltBaseAuthProvider}
import play.api.http.HeaderNames
import play.api.mvc.Request

import scala.concurrent.Future

object FakeGestaltAuthProvider {
  val ID: String = "gestalt-fake-auth-provider"
}

class FakeGestaltAuthProvider(identities: Seq[(GestaltAPICredentials,GestaltAuthResponse)]) extends GestaltBaseAuthProvider {
  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    Future.successful(for {
      creds <- request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials
      ar <- identities.find(_._1 == creds).map(_._2)
    } yield new GestaltAuthResponseWithCreds(ar.account,ar.groups,ar.rights,ar.orgId,creds))
  }
  override def id: String = FakeGestaltAuthProvider.ID
}