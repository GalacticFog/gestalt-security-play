name := """gestalt-security-play"""

organization := "com.galacticfog"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

publishTo := Some("Artifactory Realm" at "http://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "gestalt" at "http://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/")

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

libraryDependencies ++= Seq(
  "com.galacticfog" % "gestalt-security-sdk-scala_2.11" % "0.1.0-SNAPSHOT" withSources()
)

// ----------------------------------------------------------------------------
// Silhouette
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.12" % "test",
    "org.specs2" % "specs2-junit_2.11" % "2.4.17" % "test",
    "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "com.mohiva" %% "play-silhouette" % "2.0",
  "com.mohiva" %% "play-silhouette-testkit" % "2.0" % "test",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5"
)
