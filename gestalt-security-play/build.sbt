import Dependencies._

name := """gestalt-security-play"""

libraryDependencies ++= Seq(
  Library.Play.ws,
  Library.Play.json,
  Library.Gestalt.securitySdk,
  Library.Mohiva.silhouette,
  Library.Guice.multibinding,
  // 
  Library.Play.specs2         % Test
)
