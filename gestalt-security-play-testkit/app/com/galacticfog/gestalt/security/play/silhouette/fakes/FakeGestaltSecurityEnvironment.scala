package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse, GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.{Authenticator, EventBus, RequestProvider}
import com.mohiva.play.silhouette.test.FakeAuthenticatorService

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

case class FakeGestaltDelegatedSecurityEnvironment ( identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)],
                                                     securityConfig: GestaltSecurityConfig,
                                                     securityClient: GestaltSecurityClient )
                                                   ( implicit val ec: ExecutionContext )
  extends GestaltDelegatedSecurityEnvironment(securityConfig, securityClient) {

}

case class FakeGestaltFrameworkSecurityEnvironment ( identities: Seq[(GestaltAPICredentials, GestaltAuthResponseWithCreds)],
                                                     securityConfig: GestaltSecurityConfig,
                                                     securityClient: GestaltSecurityClient )
                                                   ( implicit val ec: ExecutionContext )
  extends GestaltFrameworkSecurityEnvironment(securityConfig, securityClient) {

}
