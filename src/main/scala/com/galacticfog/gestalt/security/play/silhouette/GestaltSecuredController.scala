package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, HTTP}
import com.galacticfog.gestalt.security.play.silhouette.utils.{SecurityConfig, GestaltSecurityConfig}
import com.mohiva.play.silhouette.api.services.{IdentityService, AuthenticatorService}
import com.mohiva.play.silhouette.api.{Provider, EventBus, Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import play.api.Logger
import play.api.Play.current

class GestaltSecuredController extends Silhouette[AuthAccount, DummyAuthenticator] {

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(HTTP,"localhost",9455,"0000ApiKeyNotProvided000","0000000000APISecretNotProvided0000000000",None)
  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val config: GestaltSecurityConfig = try {
    Logger.info("creating GestaltSecurityConfig")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse SecurityConfig.getSecurityConfig
    c.getOrElse {
      Logger.warn("Could not determine GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }
  Logger.info(s"bound security to ${config.protocol}://${config.host}:${config.port}, apiKey: ${config.apiKey}, appId: ${config.appId}")

  //  val client = GestaltSecurityModule.createGestaltSecurityClient(config)
  //  val accountService: AccountService = new AccountServiceImpl()
  //  val authService: AuthenticatorService[DummyAuthenticator] = GestaltSecurityModule.createAuthenticatorService()
  //  val eventBus: EventBus = EventBus()
  //  val provider: GestaltAuthProvider = GestaltSecurityModule.createGestaltSecurityProvider(config, client)
  //  val env = GestaltSecurityModule.createEnvironment(accountService, authService, eventBus, provider)
  val env = new Environment[AuthAccount,DummyAuthenticator] {
    override def identityService: IdentityService[AuthAccount] = new AccountServiceImpl()
    override def authenticatorService: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService()
    override def providers: Map[String, Provider] = Map(GestaltAuthProvider.ID -> new GestaltAuthProvider(config.appId.getOrElse("0000AppIdNotProvided0000"), GestaltSecurityClient(config.protocol,config.host,config.port,config.apiKey,config.apiSecret)))
    override def eventBus: EventBus = EventBus()
  }

}

