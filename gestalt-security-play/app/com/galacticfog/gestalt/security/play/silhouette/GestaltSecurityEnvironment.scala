package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.Logger
import play.api.libs.ws.WSClient

trait GestaltSecurityEnvironment[GI <: GestaltAuthIdentity] extends Env {
  def client: GestaltSecurityClient
  def config: GestaltSecurityConfig
  override type A = DummyAuthenticator
  override type I = GI
}

trait GestaltDelegatedSecurityEnvironment extends GestaltSecurityEnvironment[AuthAccount]

@Singleton
class DefaultGestaltDelegatedSecurityEnvironment @Inject()(wsclient: WSClient) extends GestaltDelegatedSecurityEnvironment {

  val logger = Logger(this.getClass)

  lazy val delegatedConfig = try {
    logger.info("attempting to determine GestaltSecurityConfig for delegated authentication mode")
    GestaltSecurityConfig.getSecurityConfig
      .filter(config => config.mode == DELEGATED_SECURITY_MODE && config.isWellDefined)
      .getOrElse {
        logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
        DefaultGestaltDelegatedSecurityEnvironment.FALLBACK_SECURITY_CONFIG
      }
  } catch {
    case t: Throwable =>
      logger.error(s"caught exception trying to get security config: ${t.getMessage}. Will fallback to localhost.",t)
      DefaultGestaltDelegatedSecurityEnvironment.FALLBACK_SECURITY_CONFIG
  }

  lazy val delegatedClient = new GestaltSecurityClient(
    client = wsclient,
    protocol = delegatedConfig.protocol,
    hostname = delegatedConfig.hostname,
    port = delegatedConfig.port,
    creds = GestaltBasicCredentials(delegatedConfig.apiKey,delegatedConfig.apiSecret)
  )

  override def config = delegatedConfig
  override def client = delegatedClient
}

object DefaultGestaltDelegatedSecurityEnvironment {
  val FALLBACK_SECURITY_CONFIG: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = DELEGATED_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = Some(UUID.randomUUID())
  )
}

trait GestaltFrameworkSecurityEnvironment extends GestaltSecurityEnvironment[AuthAccountWithCreds]

@Singleton
class DefaultGestaltFrameworkSecurityEnvironment @Inject()(wsclient: WSClient) extends GestaltFrameworkSecurityEnvironment {

  val logger = Logger(this.getClass)

  lazy val frameworkConfig = try {
    logger.info("attempting to determine GestaltSecurityConfig for framework authentication mode")
    GestaltSecurityConfig.getSecurityConfig
      .filter(_.isWellDefined)
      .getOrElse {
        logger.warn("could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
        DefaultGestaltFrameworkSecurityEnvironment.FALLBACK_SECURITY_CONFIG
      }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}. Will fallback to localhost.",t)
      DefaultGestaltFrameworkSecurityEnvironment.FALLBACK_SECURITY_CONFIG
  }

  lazy val frameworkClient = new GestaltSecurityClient(
    client = wsclient,
    protocol = frameworkConfig.protocol,
    hostname = frameworkConfig.hostname,
    port = frameworkConfig.port,
    creds = GestaltBasicCredentials(frameworkConfig.apiKey,frameworkConfig.apiSecret)
  )

  override def config = frameworkConfig
  override def client = frameworkClient
}

object DefaultGestaltFrameworkSecurityEnvironment {
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
