package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api.{HTTP, FRAMEWORK_SECURITY_MODE, GestaltSecurityConfig}
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.services.IdentityService
import play.api.Logger

class GestaltFrameworkSecurityModule extends AbstractModule {

  override def configure() = {
    bind(classOf[GestaltSecurityConfig]).toInstance(defaultConfig)
    bind(classOf[IdentityService[AuthAccountWithCreds]]).to(classOf[AccountServiceImplWithCreds])
  }

  lazy val FALLBACK_SECURITY_CONFIG: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = FRAMEWORK_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = None
  )

  lazy val defaultConfig: GestaltSecurityConfig = try {
    Logger.info("attempting to determine GestaltSecurityConfig for framework authentication controller")
    GestaltSecurityConfig.getSecurityConfig
      .filter(config => config.mode == FRAMEWORK_SECURITY_MODE && config.isWellDefined)
      .getOrElse {
        Logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
        FALLBACK_SECURITY_CONFIG
      }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}. Will fallback to localhost.",t)
      FALLBACK_SECURITY_CONFIG
  }
}
