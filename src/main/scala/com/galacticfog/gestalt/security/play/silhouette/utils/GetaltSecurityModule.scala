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
import play.api.Logger
import play.api.Play.current

import scala.collection.immutable.ListMap

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class GetaltSecurityModule extends AbstractModule with ScalaModule {

  def getFallbackSecurityConfig(): GestaltSecurityConfig = GestaltSecurityConfig(HTTP,"localhost",9455,"0000ApiKeyNotProvided000","0000000000APISecretNotProvided0000000000",None)
  def getSecurityConfig(): Option[GestaltSecurityConfig] = None

  /**
   * Configures the module.
   */
  def configure() {
    Logger.info("in SilhouetteModule.configure()")
    bind[AccountService].to[AccountServiceImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[HTTPLayer].to[PlayHTTPLayer]
    bind[EventBus].toInstance(EventBus())
    Logger.info("about to create GestaltSecurityConfig")
    val config = try {
      val c = getSecurityConfig() orElse SecurityConfig.getSecurityConfig()
      c.getOrElse {
        Logger.warn("Could not determine GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
        getFallbackSecurityConfig()
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"caught exception ${t.getMessage} trying to get security config",t)
        getFallbackSecurityConfig()
    }
    Logger.info(s"binding GestaltSecurityConfig to ${config.protocol}://${config.host}:${config.port}")
    bind[GestaltSecurityConfig].toInstance(config)
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
    val appId = secConfig.appId.getOrElse {
      Logger.warn("Could not determine appId in Gestalt security settings; security will not work")
      "0000AppIdNotProvided0000"
    }
    new GestaltAuthProvider(appId, gestaltSecurityClient)
  }

}
