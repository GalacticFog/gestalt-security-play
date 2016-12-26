package com.galacticfog.gestalt.security.play.silhouette.modules

import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, FRAMEWORK_SECURITY_MODE}
import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette.{AccountServiceImpl, AccountServiceImplWithCreds, AuthAccount, AuthAccountWithCreds}
import com.galacticfog.gestalt.security.play.silhouette.GestaltSecurityEnvironment
import com.galacticfog.gestalt.security.play.silhouette.GestaltDelegatedSecurityEnvironment
import com.galacticfog.gestalt.security.play.silhouette.GestaltFrameworkSecurityEnvironment
import com.galacticfog.gestalt.security.play.silhouette.GestaltAuthIdentity
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.Authenticator
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticatorService
import com.mohiva.play.silhouette.api.services.IdentityService
import play.api.Application

import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

//import com.google.inject.util.Types
//import com.google.inject.multibindings.Multibinder


class GestaltSecurityEnvironmentModule extends AbstractModule {
  
  def configure() = {
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[AuthenticatorService[DummyAuthenticator]]{}).toInstance(new DummyAuthenticatorService) 
  }
  
  @Provides def providesEnvironment(
      securityConfig: GestaltSecurityConfig,
      securityClient: GestaltSecurityClient,
      eventBus: EventBus,
      identityService: IdentityService[AuthAccountWithCreds],
      authenticatorService: AuthenticatorService[DummyAuthenticator])(
        implicit ec: ExecutionContext): GestaltSecurityEnvironment[AuthAccountWithCreds,DummyAuthenticator] = {
      
    new GestaltFrameworkSecurityEnvironment(
            securityConfig, 
            securityClient, 
            eventBus, 
            identityService, 
            authenticatorService)
  }  
  
}