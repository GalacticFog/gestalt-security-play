package com.galacticfog.gestalt.security.play.silhouette.utils

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, HTTP, HTTPS, Protocol}
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus}
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
  }

  @Provides
  def provideGestaltSecurityClient(): GestaltSecurityClient = {
    val authHost = current.configuration.getString("gestalt.authentication.hostname").getOrElse("localhost")
    val authPort = current.configuration.getInt("gestalt.authentication.port").getOrElse(9010)
    val authProtocol: Protocol = current.configuration.getString("gestalt.authentication.protocol").getOrElse("http").toUpperCase() match {
      case "HTTP" => HTTP
      case "HTTPS" => HTTPS
      case _ => throw new RuntimeException("Invalid protocol for Gestalt security: gestalt.authentication.protocol in application.conf")
    }
    val authKey = current.configuration.getString("gestalt.authentication.key").getOrElse("")
    if (authKey.isEmpty) throw new RuntimeException("Missing Gestalt authentication key: gestalt.authentication.key in application.conf")
    val authSecret = current.configuration.getString("gestalt.authentication.secret").getOrElse("")
    if (authSecret.isEmpty) throw new RuntimeException("Missing Gestalt authentication secret: gestalt.authentication.secret in application.conf")
    GestaltSecurityClient(protocol = authProtocol, hostname = authHost, port = authPort, apiKey = authKey, apiSecret = authSecret)
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
  def provideGestaltSecurityProvider(gestaltSecurityClient: GestaltSecurityClient): GestaltAuthProvider = {
    current.configuration.getString("gestalt.authentication.appId") match {
      case Some(appId) => new GestaltAuthProvider(appId,gestaltSecurityClient)
      case None => throw new RuntimeException("Could not determine appId for Gestalt authentication")
    }

  }

}
