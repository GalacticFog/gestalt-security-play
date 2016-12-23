package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, SecTypes}
import com.mohiva.play.silhouette.api.Authenticator
import com.mohiva.play.silhouette.api.services.AuthenticatorService

class FakeGestaltDelegatedSecurityModule[A <: Authenticator](fakeEnv: FakeGestaltDelegatedSecurityEnvironment[A])
                                                            (implicit clazzA: Class[A]) extends AbstractModule {
  override def configure() = {
    val tl = SecTypes.secEnv[AuthAccount,A](classOf[AuthAccount],clazzA)
    bind(tl).toInstance(fakeEnv)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
  }
}

class FakeGestaltFrameworkSecurityModule[A <: Authenticator](fakeEnv: FakeGestaltFrameworkSecurityEnvironment[A])
                                                            (implicit clazzA: Class[A]) extends AbstractModule {
  override def configure() = {
    val tl = SecTypes.secEnv[AuthAccountWithCreds,A](classOf[AuthAccountWithCreds],clazzA)
    bind(tl).toInstance(fakeEnv)
    bind(classOf[AuthenticatorService[A]]).toInstance(fakeEnv.authenticatorService)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
  }
}
