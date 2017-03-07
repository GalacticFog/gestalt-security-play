package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticatorService
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

class FakeGestaltDelegatedSecurityModule( fakeEnv: FakeGestaltDelegatedSecurityEnvironment ) extends AbstractModule with ScalaModule {
  override def configure() = {
    bind[Silhouette[GestaltDelegatedSecurityEnvironment]].to[SilhouetteProvider[GestaltDelegatedSecurityEnvironment]]
    bind[GestaltDelegatedSecurityEnvironment].toInstance(fakeEnv)
    bind[GestaltSecurityConfig].toInstance(fakeEnv.config)
    bind[GestaltSecurityClient].toInstance(fakeEnv.client)
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccount]]{}).toInstance(new AccountServiceImpl())
  }

  @Provides
  def provideDelegatedSecurityEnvironment( identityService: IdentityService[AuthAccount],
                                           securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient,
                                           eventBus: EventBus )
                                         ( implicit ec: ExecutionContext ): Environment[GestaltDelegatedSecurityEnvironment] = {
    Environment[GestaltDelegatedSecurityEnvironment](
      identityService,
      new DummyAuthenticatorService,
      Seq(new FakeGestaltDelegatedAuthProvider(fakeEnv.identities)),
      eventBus
    )
  }
}

class FakeGestaltFrameworkSecurityModule( fakeEnv: FakeGestaltFrameworkSecurityEnvironment ) extends AbstractModule with ScalaModule {
  override def configure() = {
    bind[Silhouette[GestaltFrameworkSecurityEnvironment]].to[SilhouetteProvider[GestaltFrameworkSecurityEnvironment]]
    bind[GestaltFrameworkSecurityEnvironment].toInstance(fakeEnv)
    bind[GestaltSecurityConfig].toInstance(fakeEnv.config)
    bind[GestaltSecurityClient].toInstance(fakeEnv.client)
    bind[EventBus].toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccountWithCreds]]{}).toInstance(new AccountServiceImplWithCreds())
  }

  @Provides
  def provideFrameworkSecurityEnvironment( identityService: IdentityService[AuthAccountWithCreds],
                                           securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient,
                                           eventBus: EventBus )
                                         ( implicit ec: ExecutionContext ): Environment[GestaltFrameworkSecurityEnvironment] = {
    Environment[GestaltFrameworkSecurityEnvironment](
      identityService,
      new DummyAuthenticatorService,
      Seq(new FakeGestaltFrameworkAuthProvider(fakeEnv.identities)),
      eventBus
    )
  }
}

object FakeGestaltSecurityModule {
  def apply(fakeEnv: FakeGestaltDelegatedSecurityEnvironment): AbstractModule = {
    new FakeGestaltDelegatedSecurityModule(fakeEnv)
  }
  def apply(fakeEnv: FakeGestaltFrameworkSecurityEnvironment): AbstractModule = {
    new FakeGestaltFrameworkSecurityModule(fakeEnv)
  }
}
