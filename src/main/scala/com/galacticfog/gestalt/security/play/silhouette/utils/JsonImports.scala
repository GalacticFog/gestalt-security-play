package com.galacticfog.gestalt.security.play.silhouette.utils

import com.galacticfog.gestalt.security.play.silhouette.AuthAccount
import com.galacticfog.gestalt.security.api.json.JsonImports.{acctFormat,grantFormat}
import play.api.libs.json.Json

object JsonImports {

  lazy implicit val authAccountFormat = Json.format[AuthAccount]

}
