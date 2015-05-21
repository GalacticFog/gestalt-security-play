package com.galacticfog.gestalt.security.play.silhouette.examples

import com.galacticfog.gestalt.security.api.HTTP
import com.galacticfog.gestalt.security.play.silhouette.GestaltSecuredInstanceController
import com.galacticfog.gestalt.security.play.silhouette.utils.GestaltSecurityConfig
import play.api.mvc.Action


/*
 this weird stuff is for testing the controller, per https://www.playframework.com/documentation/2.3.x/ScalaTestingWithSpecs2

 if you're not interested in testing your class in this manner, you can put the method implementations in the object below and not have the trait at all

 like so:
   object ExampleInstanceController extends GestaltSecuredInstanceController {
     ...
   }
*/
trait ExampleInstanceController {
  this: GestaltSecuredInstanceController =>

  def insecureMethod() = Action {
    Ok("insecure")
  }

  def secureMethod() = SecuredAction {
    Ok("secure")
  }
}

object ExampleInstanceController extends GestaltSecuredInstanceController with ExampleInstanceController {

  override def getSecurityConfig: Option[GestaltSecurityConfig] = Some(GestaltSecurityConfig(HTTP, "security.company.com", 9455, "securityKey", "securitySecret", Some("appId")))

}
