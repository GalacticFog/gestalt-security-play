package com.galacticfog.gestalt.security.play.silhouette

import javax.inject.Inject

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.utils.{SecurityConfig, GestaltSecurityConfig, GestaltSecurityModule}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.{EventBus, Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import play.api.Logger

class GestaltSecuredInstanceController extends Silhouette[AuthAccount, DummyAuthenticator] {

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(HTTP,"localhost",9455,"0000ApiKeyNotProvided000","0000000000APISecretNotProvided0000000000",None)
  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val config = try {
    Logger.info("creating GestaltSecurityConfig")
    val c = getSecurityConfig orElse SecurityConfig.getSecurityConfig
    c.getOrElse {
      Logger.warn("Could not determine GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception ${t.getMessage} trying to get security config",t)
      getFallbackSecurityConfig
  }

  val module = new GestaltSecurityModule
  val client = module.provideGestaltSecurityClient(config)
  val accountService: AccountService = new AccountServiceImpl()
  val authService: AuthenticatorService[DummyAuthenticator] = module.provideAuthenticatorService()
  val eventBus: EventBus = EventBus()
  val provider: GestaltAuthProvider = module.provideGestaltSecurityProvider(config, client)
  val env = module.provideEnvironment(accountService, authService, eventBus, provider)

}

