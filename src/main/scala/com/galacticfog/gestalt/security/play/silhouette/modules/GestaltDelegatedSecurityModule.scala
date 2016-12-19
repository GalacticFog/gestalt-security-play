package com.galacticfog.gestalt.security.play.silhouette.modules

import java.util.UUID

import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, FRAMEWORK_SECURITY_MODE, GestaltSecurityConfig, HTTP}
import com.galacticfog.gestalt.security.play.silhouette.{AccountServiceImpl, AuthAccount}
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.services.IdentityService
import play.api.Logger

class GestaltDelegatedSecurityModule extends AbstractModule {

  override def configure() = {
    bind(classOf[GestaltSecurityConfig]).toInstance(defaultConfig)
    bind(classOf[IdentityService[AuthAccount]]).to(classOf[AccountServiceImpl ])
  }

  lazy val FALLBACK_SECURITY_CONFIG: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = DELEGATED_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = Some(UUID.randomUUID())
  )

  lazy val defaultConfig: GestaltSecurityConfig = try {
    Logger.info("attempting to determine GestaltSecurityConfig for delegated authentication controller")
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
