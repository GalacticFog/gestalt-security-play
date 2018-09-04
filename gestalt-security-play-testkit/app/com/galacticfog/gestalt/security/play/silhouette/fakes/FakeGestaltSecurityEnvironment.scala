package com.galacticfog.gestalt.security.play.silhouette.fakes

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltAuthResponse, GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette._

case class FakeGestaltDelegatedSecurityEnvironment (identities: Seq[(GestaltAPICredentials, GestaltAuthResponse)],
                                                    securityConfig: GestaltSecurityConfig,
                                                    securityClient: GestaltSecurityClient ) extends GestaltDelegatedSecurityEnvironment {
  override def client: GestaltSecurityClient = securityClient
  override def config: GestaltSecurityConfig = securityConfig
}

case class FakeGestaltFrameworkSecurityEnvironment (identities: Seq[(GestaltAPICredentials, GestaltAuthResponseWithCreds)],
                                                    securityConfig: GestaltSecurityConfig,
                                                    securityClient: GestaltSecurityClient ) extends GestaltFrameworkSecurityEnvironment {
  override def client: GestaltSecurityClient = securityClient
  override def config: GestaltSecurityConfig = securityConfig
}
