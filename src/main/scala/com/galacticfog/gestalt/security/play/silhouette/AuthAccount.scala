package com.galacticfog.gestalt.security.play.silhouette

import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltRightGrant, GestaltAuthResponse}
import com.mohiva.play.silhouette.api.Identity

// this is, effectively, a GestaltRightGrant
case class AuthAccount(account: GestaltAccount, rights: Seq[GestaltRightGrant]) extends Identity
