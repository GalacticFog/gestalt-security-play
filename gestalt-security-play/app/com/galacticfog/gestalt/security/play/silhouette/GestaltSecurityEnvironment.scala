package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, FRAMEWORK_SECURITY_MODE, GestaltSecurityClient, GestaltSecurityConfig}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Authenticator, Environment, EventBus, RequestProvider}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import play.api.Logger

import scala.concurrent.ExecutionContext

trait GestaltSecurityEnvironment[I <: GestaltAuthIdentity, A <: Authenticator] extends Environment[I, A] {
  def client: GestaltSecurityClient
  def config: GestaltSecurityConfig
}

class GestaltDelegatedSecurityEnvironment[A <: Authenticator] @Inject() ( securityConfig: GestaltSecurityConfig,
                                                                          securityClient: GestaltSecurityClient,
                                                                          bus: EventBus,
                                                                          identitySvc: IdentityService[AuthAccount],
                                                                          authSvc: AuthenticatorService[A] )
                                                                        ( implicit ec: ExecutionContext )
  extends GestaltSecurityEnvironment[AuthAccount, A] {

  val gstltAuthProvider = securityConfig match {
    case GestaltSecurityConfig(_, _, _, _, _, _, None) =>
      Logger.error(s"GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId: ${securityConfig}")
      throw new RuntimeException("GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId.")
    case GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE, _, _, _, _, _, Some(appId)) =>
      Logger.warn("GestaltSecurityConfig configured for FRAMEWORK mode was passed to GestaltDelegatedSecurityEnvironment; this is not valid. Will use the configuration as is because it had an appId.")
      new GestaltDelegatedAuthProvider(appId, client)
    case GestaltSecurityConfig(DELEGATED_SECURITY_MODE, _, _, _, _, _, Some(appId)) =>
      new GestaltDelegatedAuthProvider(appId, client)
  }

  override def identityService: IdentityService[AuthAccount] = identitySvc

  override def authenticatorService: AuthenticatorService[A] = authSvc

  override def eventBus: EventBus = bus

  override def requestProviders: Seq[RequestProvider] = Seq(gstltAuthProvider)

  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}

class GestaltFrameworkSecurityEnvironment[A <: Authenticator] @Inject() ( securityConfig: GestaltSecurityConfig,
                                                                          securityClient: GestaltSecurityClient,
                                                                          bus: EventBus,
                                                                          identitySvc: IdentityService[AuthAccountWithCreds],
                                                                          authSvc: AuthenticatorService[A] )
                                                                        ( implicit ec: ExecutionContext )
  extends GestaltSecurityEnvironment[AuthAccountWithCreds, A] {

  val gstltAuthProvider = new GestaltFrameworkAuthProvider(client)

  override def identityService: IdentityService[AuthAccountWithCreds] = identitySvc

  override def authenticatorService: AuthenticatorService[A] = authSvc

  override def eventBus: EventBus = bus

  override def requestProviders: Seq[RequestProvider] = Seq(gstltAuthProvider)

  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}
