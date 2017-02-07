name := """gestalt-framework-auth"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.galacticfog" %% "gestalt-security-play" % "3.0.3" withSources,
  //
  specs2 % Test,
  "com.galacticfog" %% "gestalt-security-play-testkit" % "3.0.3" % Test withSources
)

resolvers ++= Seq(
    "Atlassian Releases" at "https://maven.atlassian.com/public/",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "gestalt-snapshots" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
    "gestalt-releases" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-releases-local"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
