package com.galacticfog.gestalt.security.play.silhouette.modules

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.play.silhouette._
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.{Silhouette, SilhouetteProvider}
import net.codingwell.scalaguice.ScalaModule
import play.api.Logger

class GestaltFrameworkSecurityConfigModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[Silhouette[GestaltFrameworkSecurityEnvironment]].to[SilhouetteProvider[GestaltFrameworkSecurityEnvironment]]
  }

  lazy val config = try {
    Logger.info("attempting to determine GestaltSecurityConfig for framework authentication mode")
    GestaltSecurityConfig.getSecurityConfig
      .filter(_.isWellDefined)
      .getOrElse {
        Logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
        GestaltFrameworkSecurityConfigModule.FALLBACK_SECURITY_CONFIG
      }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}. Will fallback to localhost.",t)
      GestaltFrameworkSecurityConfigModule.FALLBACK_SECURITY_CONFIG
  }

  @Provides
  def providesSecurityConfig() = config

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
