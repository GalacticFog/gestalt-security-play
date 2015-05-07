name := """gestalt-security-play"""

version := "1.0"

scalaVersion := "2.11.6"

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

resolvers ++= Seq(
  "gfi-libs-snapshots" at "http://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

libraryDependencies ++= Seq(
  "com.galacticfog" % "gestalt-security-sdk-scala_2.11" % "0.1.0-SNAPSHOT" withSources()
)

// ----------------------------------------------------------------------------
// Silhouette
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "2.0",
  "com.mohiva" %% "play-silhouette-testkit" % "2.0" % "test",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5"
)
