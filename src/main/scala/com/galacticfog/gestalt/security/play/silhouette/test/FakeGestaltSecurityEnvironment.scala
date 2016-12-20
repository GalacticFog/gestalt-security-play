package com.galacticfog.gestalt.security.play.silhouette.test

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse, GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.{Authenticator, EventBus, RequestProvider}
import com.mohiva.play.silhouette.test.FakeAuthenticatorService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.reflect.runtime.universe._

case class FakeGestaltSecurityEnvironment[T <: Authenticator: TypeTag] ( identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)],
                                                                         config: GestaltSecurityConfig,
                                                                         client: GestaltSecurityClient )
                                                                       ( implicit val ec: ExecutionContext )
  extends GestaltSecurityEnvironment[AuthAccountWithCreds,T] {

  override val identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds()(ec)

  override val authenticatorService: AuthenticatorService[T] = FakeAuthenticatorService[T]()

  override val eventBus: EventBus = EventBus()

  override val requestProviders: Seq[RequestProvider] = Seq(new FakeGestaltAuthProvider(identities))

  override implicit val executionContext: ExecutionContext = ec

}

