package com.galacticfog.gestalt.security.play.silhouette.utils

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, HTTP, HTTPS, Protocol}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{LoginInfo, Authenticator, Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.util._
import net.codingwell.scalaguice.ScalaModule
import play.api.Play.current

import scala.collection.immutable.ListMap

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[AccountService].to[AccountServiceImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[HTTPLayer].to[PlayHTTPLayer]
    bind[EventBus].toInstance(EventBus())
    bind[GestaltSecurityConfig].toInstance(
      SecurityConfig.getSecurityConfig() getOrElse(throw new RuntimeException("Could not determine Gestalt security settings from environment variables or application config"))
    )
  }

  @Provides
  def provideGestaltSecurityClient(secConfig: GestaltSecurityConfig): GestaltSecurityClient = {
    GestaltSecurityClient(
      protocol = secConfig.protocol,
      hostname = secConfig.host,
      port = secConfig.port,
      apiKey = secConfig.apiKey,
      apiSecret = secConfig.apiSecret
    )
  }

  /**
   * Provides the Silhouette environment.
   *
   * @param accountService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @param gestaltProvider The Gestalt security provider
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    accountService: AccountService,
    authenticatorService: AuthenticatorService[DummyAuthenticator],
    eventBus: EventBus,
    gestaltProvider: GestaltAuthProvider): Environment[AuthAccount, DummyAuthenticator] = {

    Environment[AuthAccount, DummyAuthenticator](
      accountService,
      authenticatorService,
      ListMap(
        gestaltProvider.id -> gestaltProvider
      ),
      eventBus
    )
  }

  /**
   * Provides the authenticator service.
   *
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(): AuthenticatorService[DummyAuthenticator] = {
    new DummyAuthenticatorService()
  }

  /**
   * Provides the Gestalt security provider.
   *
   * @return The security provider.
   */
  @Provides
  def provideGestaltSecurityProvider(secConfig: GestaltSecurityConfig, gestaltSecurityClient: GestaltSecurityClient): GestaltAuthProvider = {
    secConfig.appId match {
      case Some(appId) => new GestaltAuthProvider(appId, gestaltSecurityClient)
      case None => throw new RuntimeException("Could not determine appId in Gestalt security settings")
    }
  }

}
