package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, SecTypes, TypeLiteral}
import com.mohiva.play.silhouette.api.{Authenticator, EventBus}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}

class FakeGestaltDelegatedSecurityModule[A <: Authenticator](fakeEnv: FakeGestaltDelegatedSecurityEnvironment[A])
                                                            (implicit clazzA: Class[A]) extends AbstractModule {
  override def configure() = {
    bind(
      SecTypes.secEnv[AuthAccount, A](classOf[AuthAccount], clazzA)
    ).toInstance(fakeEnv)
    bind(
      SecTypes.authService[A](clazzA)
    ).toInstance(fakeEnv.authenticatorService)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccount]]{}).toInstance(new AccountServiceImpl())
  }
}

class FakeGestaltFrameworkSecurityModule[A <: Authenticator](fakeEnv: FakeGestaltFrameworkSecurityEnvironment[A])
                                                            (implicit clazzA: Class[A]) extends AbstractModule {
  override def configure() = {
    bind(
      SecTypes.secEnv[AuthAccountWithCreds, A](classOf[AuthAccountWithCreds], clazzA)
    ).toInstance(fakeEnv)
    bind(
      SecTypes.authService[A](clazzA)
    ).toInstance(fakeEnv.authenticatorService)
    bind(classOf[GestaltSecurityConfig]).toInstance(fakeEnv.config)
    bind(classOf[GestaltSecurityClient]).toInstance(fakeEnv.client)
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccountWithCreds]]{}).toInstance(new AccountServiceImplWithCreds())
  }
}

object FakeGestaltSecurityModule {
  def apply[A <: Authenticator](fakeEnv: FakeGestaltDelegatedSecurityEnvironment[A])(implicit ctag: reflect.ClassTag[A]): AbstractModule = {
    new FakeGestaltDelegatedSecurityModule[A](fakeEnv)(ctag.runtimeClass.asInstanceOf[Class[A]])
  }
  def apply[A <: Authenticator](fakeEnv: FakeGestaltFrameworkSecurityEnvironment[A])(implicit ctag: reflect.ClassTag[A]): AbstractModule = {
    new FakeGestaltFrameworkSecurityModule[A](fakeEnv)(ctag.runtimeClass.asInstanceOf[Class[A]])
  }
}
