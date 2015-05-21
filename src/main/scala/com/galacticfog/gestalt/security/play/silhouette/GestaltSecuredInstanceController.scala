package com.galacticfog.gestalt.security.play.silhouette

import javax.inject.Inject

import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityConfig
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.{EventBus, Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator
import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityModule

abstract class GestaltSecuredInstanceController extends Silhouette[AuthAccount, DummyAuthenticator] {

  def getSecurityConfig: GestaltSecurityConfig

  val config = this.getSecurityConfig

  val module = new GestaltSecurityModule
  val client = module.provideGestaltSecurityClient(config)
  val accountService: AccountService = new AccountServiceImpl()
  val authService: AuthenticatorService[DummyAuthenticator] = module.provideAuthenticatorService()
  val eventBus: EventBus = EventBus()
  val provider: GestaltAuthProvider = module.provideGestaltSecurityProvider(config, client)
  val env = module.provideEnvironment(accountService, authService, eventBus, provider)

}

