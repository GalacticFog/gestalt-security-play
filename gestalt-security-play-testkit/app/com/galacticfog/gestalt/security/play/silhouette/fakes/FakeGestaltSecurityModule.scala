package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.{Authenticator, EventBus}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

class FakeGestaltDelegatedSecurityModule( fakeEnv: FakeGestaltDelegatedSecurityEnvironment ) extends AbstractModule {
  override def configure() = {
    bind(classOf[GestaltSecurityEnvironment]).toInstance(fakeEnv)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccount]]{}).toInstance(new AccountServiceImpl())
  }
}

class FakeGestaltFrameworkSecurityModule( fakeEnv: FakeGestaltFrameworkSecurityEnvironment ) extends AbstractModule {
  override def configure() = {
    bind(classOf[GestaltSecurityEnvironment]).toInstance(fakeEnv)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccountWithCreds]]{}).toInstance(new AccountServiceImplWithCreds())
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
