package com.google.inject

import com.galacticfog.gestalt.security.play.silhouette.{GestaltAuthIdentity, GestaltSecurityEnvironment}
import com.mohiva.play.silhouette.api.Authenticator
import util.Types.newParameterizedType

object SecTypes {
  def secEnv[I <: GestaltAuthIdentity, A <: Authenticator](implicit clazzI: Class[I], clazzA: Class[A]): TypeLiteral[GestaltSecurityEnvironment[I,A]] = {
    val pt = newParameterizedType(classOf[GestaltSecurityEnvironment[I,A]], clazzI, clazzA)
    new TypeLiteral[GestaltSecurityEnvironment[I, A]](pt)
  }
}
