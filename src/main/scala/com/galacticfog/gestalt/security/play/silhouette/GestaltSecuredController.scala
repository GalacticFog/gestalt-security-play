package com.galacticfog.gestalt.security.play.silhouette

import play.api.i18n.MessagesApi
import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api._

abstract class GestaltSecuredController[A <: Authenticator](mAPI: MessagesApi,
                                                            client: GestaltSecurityClient,
                                                            environment: Environment[AuthAccount, A]) extends Silhouette[AuthAccount, A] {

  override val messagesApi: MessagesApi = mAPI

  override val env: Environment[AuthAccount, A] = environment

  val securityClient: GestaltSecurityClient = client

}



