package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, HTTP}
import com.galacticfog.gestalt.security.api.GestaltSecurityConfig
import com.mohiva.play.silhouette.api.services.{IdentityService, AuthenticatorService}
import com.mohiva.play.silhouette.api.{Provider, EventBus, Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.Json

import scala.util.Try

class GestaltSecuredController(val meta: Option[Gestalt]) extends Silhouette[AuthAccount, DummyAuthenticator] {

  def this() = this(meta = None)

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(HTTP,"localhost",9455,"0000ApiKeyNotProvided000","0000000000APISecretNotProvided0000000000",None)
  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val securityConfig: GestaltSecurityConfig = try {
    Logger.info("creating GestaltSecurityConfig")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse GestaltSecurityConfig.getSecurityConfig(meta)
    c.getOrElse {
      Logger.warn("Could not determine GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }
  Logger.info(s"bound security to ${securityConfig.protocol}://${securityConfig.host}:${securityConfig.port}, apiKey: ${securityConfig.apiKey}, appId: ${securityConfig.appId}")
  
  val securityClient: GestaltSecurityClient = GestaltSecurityClient(securityConfig.protocol,securityConfig.host,securityConfig.port,securityConfig.apiKey,securityConfig.apiSecret)

  // override: needed by Silhouette
  val env = new Environment[AuthAccount,DummyAuthenticator] {
    override def identityService: IdentityService[AuthAccount] = new AccountServiceImpl()
    override def authenticatorService: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService()
    override def providers: Map[String, Provider] = Map(GestaltAuthProvider.ID -> new GestaltAuthProvider(securityConfig.appId.getOrElse("0000AppIdNotProvided0000"), securityClient))
    override def eventBus: EventBus = EventBus()
  }

}

