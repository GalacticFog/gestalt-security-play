package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{DELEGATED_SECURITY_MODE, FRAMEWORK_SECURITY_MODE, GestaltSecurityClient, GestaltSecurityConfig}
import com.google.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import play.api.Logger

import scala.concurrent.ExecutionContext

trait GestaltSecurityEnvironment {
  def client: GestaltSecurityClient
  def config: GestaltSecurityConfig
}

class GestaltDelegatedSecurityEnvironment( securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient )
                                         ( implicit ec: ExecutionContext )
  extends Env with GestaltSecurityEnvironment {

  val gstltAuthProvider = securityConfig match {
    case GestaltSecurityConfig(_, _, _, _, _, _, None,_) =>
      Logger.error(s"GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId: ${securityConfig}")
      throw new RuntimeException("GestaltSecurityConfig passed to GestaltDelegatedSecurityEnvironment without appId.")
    case GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE, _, _, _, _, _, Some(appId), _) =>
      Logger.warn("GestaltSecurityConfig configured for FRAMEWORK mode was passed to GestaltDelegatedSecurityEnvironment; this is not valid. Will use the configuration as is because it had an appId.")
      new GestaltDelegatedAuthProvider(appId, client)
    case GestaltSecurityConfig(DELEGATED_SECURITY_MODE, _, _, _, _, _, Some(appId), _) =>
      new GestaltDelegatedAuthProvider(appId, client)
  }

//  override def requestProviders: Seq[RequestProvider] = Seq(gstltAuthProvider)
//
//  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}

class GestaltFrameworkSecurityEnvironment( securityConfig: GestaltSecurityConfig,
                                           securityClient: GestaltSecurityClient )
                                         ( implicit ec: ExecutionContext )
  extends Env with GestaltSecurityEnvironment {

  val gstltAuthProvider = new GestaltFrameworkAuthProvider(client)

//  override def requestProviders: Seq[RequestProvider] = Seq(gstltAuthProvider)
//
//  override implicit val executionContext: ExecutionContext = ec

  override def config = securityConfig

  override def client = securityClient
}
