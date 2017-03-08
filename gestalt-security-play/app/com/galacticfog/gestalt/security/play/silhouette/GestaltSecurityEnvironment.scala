package com.galacticfog.gestalt.security.play.silhouette

import javax.inject.Inject

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

trait GestaltSecurityEnvironment[GI <: GestaltAuthIdentity] extends Env {
  def client: GestaltSecurityClient
  def config: GestaltSecurityConfig
  override type A = DummyAuthenticator
  override type I = GI
}

class GestaltDelegatedSecurityEnvironment @Inject() ( securityConfig: GestaltSecurityConfig,
                                                      securityClient: GestaltSecurityClient )
  extends GestaltSecurityEnvironment[AuthAccount] {

  override def config = securityConfig
  override def client = securityClient
}

class GestaltFrameworkSecurityEnvironment @Inject() ( securityConfig: GestaltSecurityConfig,
                                                      securityClient: GestaltSecurityClient )
  extends GestaltSecurityEnvironment[AuthAccountWithCreds] {

  override def config = securityConfig
  override def client = securityClient
}
