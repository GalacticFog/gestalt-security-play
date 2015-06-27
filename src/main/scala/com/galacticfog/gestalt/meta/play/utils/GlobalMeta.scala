package com.galacticfog.gestalt.meta.play.utils

import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.io.GestaltConfig.GestaltContext
import play.api.Logger
import play.api.libs.json.Json

trait GlobalMeta {
  this : com.galacticfog.gestalt.meta.play.utils.GlobalMeta with play.api.GlobalSettings =>

  val meta: Gestalt = new Gestalt()
  meta.local.context.foreach { (c: GestaltContext) => Logger.info(Json.prettyPrint(c.toJson)) }
}
