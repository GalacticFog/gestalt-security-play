package com.galacticfog.gestalt.security.play.silhouette.modules

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette.{AccountServiceImpl, AccountServiceImplWithCreds, AuthAccount, AuthAccountWithCreds}
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticator, DummyAuthenticatorService}
import play.api.Application
import scala.concurrent.ExecutionContext
import play.api.libs.concurrent.Execution.Implicits._

class GestaltSecurityModule extends AbstractModule {

  override def configure() = {
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccount]]{}).toInstance(new AccountServiceImpl())
    bind(new TypeLiteral[IdentityService[AuthAccountWithCreds]]{}).toInstance(new AccountServiceImplWithCreds())
  }

  @Provides
  def providesSecurityClient(config: GestaltSecurityConfig)(implicit application: Application) = {
    GestaltSecurityClient(config)(application)
  }

}
