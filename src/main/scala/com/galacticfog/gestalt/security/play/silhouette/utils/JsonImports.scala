package com.galacticfog.gestalt.security.play.silhouette.utils

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import play.api.libs.json.Json

object JsonImports {

  val authAccountFormat = Json.format[AuthAccount]

}
