package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse, GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.{Authenticator, EventBus, RequestProvider}
import com.mohiva.play.silhouette.test.FakeAuthenticatorService

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

case class FakeGestaltDelegatedSecurityEnvironment[T <: Authenticator: TypeTag] ( identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)],
                                                                                  config: GestaltSecurityConfig,
                                                                                  client: GestaltSecurityClient )
                                                                                ( implicit val ec: ExecutionContext )
  extends GestaltSecurityEnvironment[AuthAccount,T] {

  override val identityService: IdentityService[AuthAccount] = new AccountServiceImpl

  override val authenticatorService: AuthenticatorService[T] = FakeAuthenticatorService[T]()

  override val eventBus: EventBus = EventBus()

  override val requestProviders: Seq[RequestProvider] = Seq(new FakeGestaltDelegatedAuthProvider(identities))

  override implicit val executionContext: ExecutionContext = ec

}

case class FakeGestaltFrameworkSecurityEnvironment[T <: Authenticator: TypeTag] ( identities: Seq[(GestaltAPICredentials, GestaltAuthResponseWithCreds)],
                                                                                  config: GestaltSecurityConfig,
                                                                                  client: GestaltSecurityClient )
                                                                                ( implicit val ec: ExecutionContext )
  extends GestaltSecurityEnvironment[AuthAccountWithCreds,T] {

  override val identityService: IdentityService[AuthAccountWithCreds] = new AccountServiceImplWithCreds

  override val authenticatorService: AuthenticatorService[T] = FakeAuthenticatorService[T]()

  override val eventBus: EventBus = EventBus()

  override val requestProviders: Seq[RequestProvider] = Seq(new FakeGestaltFrameworkAuthProvider(identities))

  override implicit val executionContext: ExecutionContext = ec

}
