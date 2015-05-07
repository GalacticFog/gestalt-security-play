package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltAuthResponse, GestaltAccount}
import com.mohiva.play.silhouette.api.Identity

case class AuthAccount(gestaltAuthResponse: GestaltAuthResponse) extends Identity {
}
