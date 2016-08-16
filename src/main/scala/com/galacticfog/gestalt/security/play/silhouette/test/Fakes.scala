package com.galacticfog.gestalt.security.play.silhouette.test

import com.galacticfog.gestalt.security.api.{GestaltAuthResponse, GestaltSecurityClient, GestaltAPICredentials}
import com.galacticfog.gestalt.security.play.silhouette.{AccountServiceImplWithCreds, AuthAccountWithCreds, GestaltAuthResponseWithCreds, GestaltBaseAuthProvider}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.{Environment, EventBus, Authenticator}
import com.mohiva.play.silhouette.test.FakeAuthenticatorService
import play.api.http.HeaderNames
import play.api.mvc.Request

import scala.concurrent.Future
import scala.reflect.runtime.universe._

object FakeGestaltAuthProvider {
  val ID: String = "gestalt-fake-auth-provider"
}

class FakeGestaltAuthProvider(identities: Seq[(GestaltAPICredentials,GestaltAuthResponse)], client: GestaltSecurityClient) extends GestaltBaseAuthProvider(client) {
  override def gestaltAuthImpl[B](request: Request[B]): Future[Option[GestaltAuthResponse]] = {
    Future.successful(for {
      creds <- request.headers.get(HeaderNames.AUTHORIZATION) flatMap GestaltAPICredentials.getCredentials
      ar <- identities.find(_._1 == creds).map(_._2)
    } yield new GestaltAuthResponseWithCreds(ar.account,ar.groups,ar.rights,ar.orgId,creds))
  }
  override def id: String = FakeGestaltAuthProvider.ID
}

case class FakeGestaltSecurityEnvironment[T <: Authenticator: TypeTag](identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)],
                                                                       client: GestaltSecurityClient)
  extends Environment[AuthAccountWithCreds, T] {

  val identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds()

  val eventBus: EventBus = EventBus()

  val authenticatorService: AuthenticatorService[T] = FakeAuthenticatorService[T]()

  val providers = Map(
    FakeGestaltAuthProvider.ID -> new FakeGestaltAuthProvider(identities, client)
  )
}

