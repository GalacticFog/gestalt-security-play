package com.galacticfog.gestalt.meta.play.utils

import com.galacticfog.gestalt.Gestalt

trait GlobalMeta {
  this : com.galacticfog.gestalt.meta.play.utils.GlobalMeta with play.api.GlobalSettings =>

  val meta: Gestalt = new Gestalt()
}
