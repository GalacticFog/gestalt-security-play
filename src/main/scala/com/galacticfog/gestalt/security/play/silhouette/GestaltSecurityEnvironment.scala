package com.galacticfog.gestalt.security.play.silhouette

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Authenticator, EventBus, RequestProvider}
import com.mohiva.play.silhouette.api.services.{IdentityService, AuthenticatorService}
import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext

trait GestaltSecurityEnvironment[I <: GestaltAuthIdentity, A <: Authenticator] extends Environment[I, A] {

}

class GestaltDelegatedSecurityEnvironment[A <: Authenticator : TypeTag] @Inject() ()(implicit ec: ExecutionContext) extends GestaltSecurityEnvironment[AuthAccount, A] {

  override def identityService: IdentityService[AuthAccount] = ???

  override def authenticatorService: AuthenticatorService[A] = ???

  override def eventBus: EventBus = ???

  override def requestProviders: Seq[RequestProvider] = ???

  override implicit val executionContext: ExecutionContext = ec
}

class GestaltFrameworkSecurityEnvironment[A <: Authenticator : TypeTag] @Inject() ()(implicit ec: ExecutionContext) extends GestaltSecurityEnvironment[AuthAccountWithCreds, A] {

  override def identityService: IdentityService[AuthAccountWithCreds] = ???

  override def authenticatorService: AuthenticatorService[A] = ???

  override def eventBus: EventBus = ???

  override def requestProviders: Seq[RequestProvider] = ???

  override implicit val executionContext: ExecutionContext = ec
}
