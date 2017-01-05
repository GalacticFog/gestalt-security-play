package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse}
import com.galacticfog.gestalt.security.play.silhouette.{GestaltAuthIdentity, GestaltAuthResponseWithCreds, GestaltBaseAuthProvider}
import play.api.http.HeaderNames
import play.api.mvc.Request

import scala.concurrent.Future

object FakeGestaltAuthProvider {
  val ID: String = "gestalt-fake-auth-provider"
}

class FakeGestaltDelegatedAuthProvider(identities: Seq[(GestaltAPICredentials,GestaltAuthResponse)]) extends GestaltBaseAuthProvider {
  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    Future.successful(for {
      creds <- request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials
      ar <- identities.find(_._1 == creds).map(_._2)
    } yield ar)
  }
  override def id: String = FakeGestaltAuthProvider.ID
}

class FakeGestaltFrameworkAuthProvider(identities: Seq[(GestaltAPICredentials,GestaltAuthResponseWithCreds)]) extends GestaltBaseAuthProvider {
  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponseWithCreds]] = {
    Future.successful(for {
      creds <- request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials
      ar <- identities.find(_._1 == creds).map(_._2)
    } yield ar)
  }
  override def id: String = FakeGestaltAuthProvider.ID
}
