package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Authenticator, Environment, EventBus, RequestProvider}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}

import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext

trait GestaltSecurityEnvironment[I <: GestaltAuthIdentity, A <: Authenticator] extends Environment[I, A] {
  def client: GestaltSecurityClient
  def config: GestaltSecurityConfig
}

class GestaltDelegatedSecurityEnvironment[A <: Authenticator : TypeTag] @Inject() (securityConfig: GestaltSecurityConfig, securityClient: GestaltSecurityClient)
                                                                                  (implicit ec: ExecutionContext) extends GestaltSecurityEnvironment[AuthAccount, A] {

  override def identityService: IdentityService[AuthAccount] = ???

  override def authenticatorService: AuthenticatorService[A] = ???

  override def eventBus: EventBus = ???

  override def requestProviders: Seq[RequestProvider] = ???

  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}

class GestaltFrameworkSecurityEnvironment[A <: Authenticator : TypeTag] @Inject() (securityConfig: GestaltSecurityConfig, securityClient: GestaltSecurityClient)
                                                                                  (implicit ec: ExecutionContext) extends GestaltSecurityEnvironment[AuthAccountWithCreds, A] {

  override def identityService: IdentityService[AuthAccountWithCreds] = ???

  override def authenticatorService: AuthenticatorService[A] = ???

  override def eventBus: EventBus = ???

  override def requestProviders: Seq[RequestProvider] = ???

  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}
