package com.galacticfog.gestalt.security.play.silhouette

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

abstract class GestaltSecuredController
  extends Silhouette[AuthAccount, DummyAuthenticator] {

  @Inject implicit val env: Environment[AuthAccount,DummyAuthenticator] = null
}
