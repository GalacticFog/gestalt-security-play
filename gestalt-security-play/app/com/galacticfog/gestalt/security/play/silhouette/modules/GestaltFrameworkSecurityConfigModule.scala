package com.galacticfog.gestalt.security.play.silhouette.modules

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticator, DummyAuthenticatorService}
import play.api.{Application, Logger}

import scala.concurrent.ExecutionContext

class GestaltFrameworkSecurityConfigModule extends AbstractModule {

  override def configure() = {
  }

  @Provides
  def providesSecurityConfig()(implicit application: Application) = {
    try {
      Logger.info("attempting to determine GestaltSecurityConfig for framework authentication controller")
      GestaltSecurityConfig.getSecurityConfig
        .filter(config => config.mode == FRAMEWORK_SECURITY_MODE && config.isWellDefined)
        .getOrElse {
          Logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
          GestaltFrameworkSecurityConfigModule.FALLBACK_SECURITY_CONFIG
        }
    } catch {
      case t: Throwable =>
        Logger.error(s"caught exception trying to get security config: ${t.getMessage}. Will fallback to localhost.",t)
        GestaltFrameworkSecurityConfigModule.FALLBACK_SECURITY_CONFIG
    }
  }

}

object GestaltFrameworkSecurityConfigModule {
  val FALLBACK_SECURITY_CONFIG: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = None
  )
}
