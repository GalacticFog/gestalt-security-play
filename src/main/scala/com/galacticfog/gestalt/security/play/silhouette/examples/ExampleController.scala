package com.galacticfog.gestalt.security.play.silhouette.examples

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.GestaltSecuredController
import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityConfig
import play.api.Logger
import play.api.mvc.Action

object ExampleController extends GestaltSecuredController {

  val exampleConfig = GestaltSecurityConfig(HTTP, "security.company.com", 9455, "securityKey", "securitySecret", Some("appId"))

  override def getSecurityConfig: Option[GestaltSecurityConfig] = {
    Logger.info("ExampleInstanceController returning override config")
    Some(exampleConfig)
  }

  def insecureMethod() = Action {
    Ok("insecure")
  }

  def secureMethod() = SecuredAction {
    Ok("secure")
  }

}
