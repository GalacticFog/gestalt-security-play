package com.galacticfog.gestalt.security.play.silhouette.modules

import com.galacticfog.gestalt.security.api.{GestaltSecurityClient, GestaltSecurityConfig}
import com.galacticfog.gestalt.security.play.silhouette.{DefaultGestaltDelegatedSecurityEnvironment, GestaltDelegatedSecurityEnvironment, GestaltFrameworkSecurityEnvironment}
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Silhouette, SilhouetteProvider}
import net.codingwell.scalaguice.ScalaModule

class GestaltDelegatedSecurityConfigModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[Silhouette[GestaltDelegatedSecurityEnvironment]].to[SilhouetteProvider[GestaltDelegatedSecurityEnvironment]]
    bind[GestaltDelegatedSecurityEnvironment].to[DefaultGestaltDelegatedSecurityEnvironment]
    bind[GestaltFrameworkSecurityEnvironment].toInstance(new GestaltFrameworkSecurityEnvironment {
      override def client: GestaltSecurityClient = throw new RuntimeException("gestalt-security-play not configured for Framework mode; you should probably not be using GestaltDelegatedSecurityConfigModule")
      override def config: GestaltSecurityConfig = throw new RuntimeException("gestalt-security-play not configured for Framework mode; you should probably not be using GestaltDelegatedSecurityConfigModule")
    })
  }

}

