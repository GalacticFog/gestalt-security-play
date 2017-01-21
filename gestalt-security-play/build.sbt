import Dependencies._

name := """gestalt-security-play"""

libraryDependencies ++= Seq(
  Library.Play.ws,
  Library.Play.json,
  Library.Gestalt.securitySdk,
  Library.Mohiva.silhouette,
  //
  Library.Play.specs2         % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.4.2" % Test
)
