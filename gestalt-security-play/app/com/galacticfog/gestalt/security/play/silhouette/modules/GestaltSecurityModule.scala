package com.galacticfog.gestalt.security.play.silhouette.modules

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticatorService
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.ws.WSClient
import play.api.{Application, Logger}

import scala.concurrent.ExecutionContext

class GestaltSecurityModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(new TypeLiteral[IdentityService[AuthAccount]]{}).toInstance(new AccountServiceImpl())
    bind(new TypeLiteral[IdentityService[AuthAccountWithCreds]]{}).toInstance(new AccountServiceImplWithCreds())
  }

  @Provides
  def providesSecurityClient(config: GestaltSecurityConfig, wsclient: WSClient) = {
    new GestaltSecurityClient(
      client = wsclient,
      protocol = config.protocol,
      hostname = config.hostname,
      port = config.port,
      creds = GestaltBasicCredentials(config.apiKey,config.apiSecret)
    )
  }

  @Provides
  def provideGestaltDelegatedAuthProvider( securityConfig: GestaltSecurityConfig,
                                  securityClient: GestaltSecurityClient ): GestaltDelegatedAuthProvider = {
    securityConfig match {
      case GestaltSecurityConfig(_, _, _, _, _, _, None,_) =>
        Logger.error(s"GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId: ${securityConfig}")
        throw new RuntimeException("GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId.")
      case GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE, _, _, _, _, _, Some(appId), _) =>
        Logger.warn("GestaltSecurityConfig configured for FRAMEWORK mode was passed to GestaltDelegatedSecurityEnvironment; this is not valid. Will use the configuration as is because it had an appId.")
        new GestaltDelegatedAuthProvider(appId, securityClient)
      case GestaltSecurityConfig(DELEGATED_SECURITY_MODE, _, _, _, _, _, Some(appId), _) =>
        new GestaltDelegatedAuthProvider(appId, securityClient)
    }
  }

  @Provides
  def provideGestaltFrameworkAuthProvider( securityClient: GestaltSecurityClient ): GestaltFrameworkAuthProvider = {
    new GestaltFrameworkAuthProvider(securityClient)
  }

  @Provides
  def provideDelegatedSecurityEnvironment( identityService: IdentityService[AuthAccount],
                                           securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient,
                                           authProvider: GestaltDelegatedAuthProvider,
                                           eventBus: EventBus )
                                         ( implicit ec: ExecutionContext ): Environment[GestaltDelegatedSecurityEnvironment] = {
    Environment[GestaltDelegatedSecurityEnvironment](
      identityService,
      new DummyAuthenticatorService,
      Seq(authProvider),
      eventBus
    )
  }

  @Provides
  def provideFrameworkSecurityEnvironment( identityService: IdentityService[AuthAccountWithCreds],
                                           securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient,
                                           authProvider: GestaltFrameworkAuthProvider,
                                           eventBus: EventBus )
                                         ( implicit ec: ExecutionContext ): Environment[GestaltFrameworkSecurityEnvironment] = {
    Environment[GestaltFrameworkSecurityEnvironment](
      identityService,
      new DummyAuthenticatorService,
      Seq(authProvider),
      eventBus
    )
  }

}