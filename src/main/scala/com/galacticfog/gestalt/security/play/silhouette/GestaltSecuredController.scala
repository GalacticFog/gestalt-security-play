package com.galacticfog.gestalt.security.play.silhouette

import javax.inject.Inject
import play.api.i18n.MessagesApi
import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.mohiva.play.silhouette.api.services.{IdentityService, AuthenticatorService}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.{DummyAuthenticatorService, DummyAuthenticator}
import play.api.Logger
import play.api.Play.current
//import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class AuthAccount(account: GestaltAccount, groups: Seq[ResourceLink], rights: Seq[GestaltRightGrant]) extends Identity

class GestaltSecuredController @Inject()(val messagesApi: MessagesApi) extends Silhouette[AuthAccount, DummyAuthenticator] {

  def getFallbackSecurityConfig: GestaltSecurityConfig = GestaltSecurityConfig(
    mode = DELEGATED_SECURITY_MODE,
    protocol = HTTP,
    hostname = "localhost",
    port = 9455,
    apiKey = UUID.randomUUID().toString,
    apiSecret = "00000noAPISecret00000000",
    appId = Some(UUID.randomUUID())
  )

  def getSecurityConfig: Option[GestaltSecurityConfig] = None

  val securityConfig: GestaltSecurityConfig = try {
    Logger.info("attempting to determine GestaltSecurityConfig for delegated authentication controller")
    val c: Option[GestaltSecurityConfig] = getSecurityConfig orElse GestaltSecurityConfig.getSecurityConfig
    c.flatMap( config =>
      if (config.mode == DELEGATED_SECURITY_MODE && config.isWellDefined) Some(config)
      else None
    ).getOrElse {
      Logger.warn("Could not determine suitable GestaltSecurityConfig; relying on getFallbackSecurityConfig()")
      getFallbackSecurityConfig
    }
  } catch {
    case t: Throwable =>
      Logger.error(s"caught exception trying to get security config: ${t.getMessage}",t)
      getFallbackSecurityConfig
  }
  
  Logger.info(s"bound security in delegated mode to ${securityConfig.protocol}://${securityConfig.hostname}:${securityConfig.port}, apiKey: ${securityConfig.apiKey}, appId: ${securityConfig.appId}")

  val securityClient: GestaltSecurityClient = GestaltSecurityClient(securityConfig)
  val authProvider = new GestaltAuthProvider(securityConfig.appId.getOrElse(UUID.randomUUID()), securityClient)

  // override for Silhouette
  val env = new Environment[AuthAccount,DummyAuthenticator] {
    implicit val executionContext: scala.concurrent.ExecutionContext = ???
    override def identityService: IdentityService[AuthAccount] = new AccountServiceImpl()
    override def authenticatorService: AuthenticatorService[DummyAuthenticator] = new DummyAuthenticatorService()
    //override def providers: Map[String, Provider] = Map(authProvider.id -> authProvider)
    override def requestProviders: Seq[com.mohiva.play.silhouette.api.RequestProvider] = ???
    override def eventBus: EventBus = EventBus()
  }

}



