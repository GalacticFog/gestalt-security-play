import Dependencies._

name := """gestalt-security-play"""

libraryDependencies ++= Seq(
  Library.Play.ws,
  Library.Play.json,
  Library.Gestalt.securitySdk,
  Library.Mohiva.silhouette,
  "net.codingwell" %% "scala-guice" % "4.1.0",
  //
  Library.Play.specs2         % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % Test
)
