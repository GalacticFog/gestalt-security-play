package com.galacticfog.gestalt.security.play.silhouette.test

import com.galacticfog.gestalt.security.api.{GestaltAuthResponse, GestaltAPICredentials}
import com.galacticfog.gestalt.security.play.silhouette._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.{RequestProvider, EventBus, Authenticator}
import com.mohiva.play.silhouette.test.FakeAuthenticatorService
import play.api.http.HeaderNames
import play.api.mvc.Request

import scala.concurrent.{ExecutionContextExecutor, ExecutionContext, Future}
import scala.reflect.runtime.universe._

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

case class FakeGestaltSecurityEnvironment[T <: Authenticator: TypeTag]
                                         (identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)])
                                         (implicit val ec: ExecutionContextExecutor)
                                         extends GestaltSecurityEnvironment[AuthAccountWithCreds,T] {

  override val identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds()

  override val authenticatorService: AuthenticatorService[T] = FakeAuthenticatorService[T]()

  override val eventBus: EventBus = EventBus()

  override val requestProviders: Seq[RequestProvider] = Seq(new FakeGestaltAuthProvider(identities))

  override implicit val executionContext: ExecutionContext = ec
}

