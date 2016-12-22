package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, TypeLiteral}
import com.mohiva.play.silhouette.api.Authenticator
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

class FakeGestaltSecurityModule(fakeEnv: GestaltSecurityEnvironment[_,_]) extends AbstractModule {
  override def configure() = {
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
  }
}

class FakeGestaltFrameworkSecurityModule(fakeEnv: FakeGestaltFrameworkSecurityEnvironment[DummyAuthenticator]) extends AbstractModule {
  override def configure() = {
//    bind(new TypeLiteral[GestaltSecurityEnvironment[AuthAccountWithCreds,DummyAuthenticator]]{}).toInstance(fakeEnv)
    bind(classOf[GestaltSecurityEnvironment[_,_]]).toInstance(fakeEnv)
    bind(classOf[AuthenticatorService[DummyAuthenticator]]).toInstance(fakeEnv.authenticatorService)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
  }
}
