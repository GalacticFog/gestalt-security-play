package com.galacticfog.gestalt.security.play.silhouette.examples

import com.galacticfog.gestalt.security.play.silhouette.GestaltSecuredController
import play.api.mvc.Action

class ExampleController extends GestaltSecuredController {

  def insecureMethod() = Action {
    Ok("insecure")
  }

  def secureMethod() = SecuredAction {
    Ok("secure")
  }

}
