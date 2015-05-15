package com.galacticfog.gestalt.security.play.silhouette.utils

import com.galacticfog.gestalt.security.api.{HTTP, HTTPS, Protocol}
import play.api.Application

case class GestaltSecurityConfig(val protocol: Protocol,
                                 val host: String,
                                 val port: Int,
                                 val apiKey: String,
                                 val apiSecret: String,
                                 val appId: Option[String])

object SecurityConfig {

  val ePROTOCOL = "GESTALT_SECURITY_PROTOCOL"
  val eHOSTNAME = "GESTALT_SECURITY_HOSTNAME"
  val ePORT     = "GESTALT_SECURITY_PORT"
  val eKEY      = "GESTALT_SECURITY_KEY"
  val eSECRET   = "GESTALT_SECURITY_SECRET"
  val eAPPID    = "GESTALT_SECURITY_APPID"

  val fPROTOCOL = "GESTALT_SECURITY_PROTOCOL"
  val fHOSTNAME = "GESTALT_SECURITY_HOSTNAME"
  val fPORT     = "GESTALT_SECURITY_PORT"
  val fKEY      = "GESTALT_SECURITY_KEY"
  val fSECRET   = "GESTALT_SECURITY_SECRET"
  val fAPPID    = "GESTALT_SECURITY_APPID"

  def getEnv(name: String): Option[String] = scala.util.Properties.envOrNone(name)

  def checkProtocol(proto: String): Protocol = proto match {
    case "HTTP" => HTTP
    case "http" => HTTP
    case "HTTPS" => HTTPS
    case "https" => HTTPS
    case _ => throw new RuntimeException("Invalid protocol for Gestalt security")
  }

  def getConfigFromFile(): Option[GestaltSecurityConfig] = {
//    def getConfigFromFile()(implicit app: Application): Option[GestaltSecurityConfig] = {
//    play.api.Configuration
//    val conf = app.configuration
//    for {
//      proto  <- conf.getString(fPROTOCOL) orElse(Some("http")) map checkProtocol
//      host   <- conf.getString(fHOSTNAME)
//      port   <- conf.getInt(fPORT) orElse conf.getString(fPORT).map{_.toInt}
//      key    <- conf.getString(fKEY)
//      secret <- conf.getString(fSECRET)
//      appId   = conf.getString(fAPPID)
//    } yield GestaltSecurityConfig(protocol=proto, host=host, port=port.toInt, apiKey=key, apiSecret=secret, appId=appId)
    None
  }

  def getConfigFromEnv(): Option[GestaltSecurityConfig] = {
    for {
      proto  <- getEnv(ePROTOCOL) orElse(Some("http")) map checkProtocol
      host   <- getEnv(eHOSTNAME)
      port   <- getEnv(ePORT)
      key    <- getEnv(eKEY)
      secret <- getEnv(eSECRET)
      appId   = getEnv(eAPPID)
    } yield GestaltSecurityConfig(protocol=proto, host=host, port=port.toInt, apiKey=key, apiSecret=secret, appId=appId)
  }

  def getSecurityConfig(): Option[GestaltSecurityConfig] = {
    getConfigFromEnv() orElse getConfigFromFile()
  }

}
