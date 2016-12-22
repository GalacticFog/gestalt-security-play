package com.galacticfog.gestalt.security.play.silhouette

import java.util.UUID

import com.galacticfog.gestalt.security.api.{GestaltAPICredentials, GestaltRightGrant, ResourceLink, GestaltAccount}
import com.mohiva.play.silhouette.api.Identity

trait GestaltAuthIdentity extends Identity {
  def account: GestaltAccount
  def groups: Seq[ResourceLink]
  def rights: Seq[GestaltRightGrant]
}

case class AuthAccountWithCreds( account: GestaltAccount,
                                 groups: Seq[ResourceLink],
                                 rights: Seq[GestaltRightGrant],
                                 creds: GestaltAPICredentials,
                                 authenticatingOrgId: UUID) extends GestaltAuthIdentity

case class AuthAccount( account: GestaltAccount,
                        groups: Seq[ResourceLink],
                        rights: Seq[GestaltRightGrant]) extends GestaltAuthIdentity
