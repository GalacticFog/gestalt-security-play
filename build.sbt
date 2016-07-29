name := """gestalt-security-play"""

organization := "com.galacticfog"

version := "2.2.4-SNAPSHOT"

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-unchecked", "-deprecation", "-feature",
  "-language:postfixOps", "-language:implicitConversions"
)

resolvers ++= Seq(
  "gestalt-snapshots" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "gestalt-releases" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-releases-local",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

publishTo <<= version { (v: String) =>
  val ao = "https://galacticfog.artifactoryonline.com/galacticfog/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("publish-gf-snapshots" at ao + "libs-snapshots-local;build.timestamp=" + new java.util.Date().getTime)
  else
    Some("publish-gf-releases"  at ao + "libs-releases-local")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

libraryDependencies ++= Seq(
  "com.galacticfog" %% "gestalt-security-sdk-scala" % "2.2.4-SNAPSHOT" withSources()
)

// MockWS for testing
libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "2.3.0" % "test" withSources()

// ----------------------------------------------------------------------------
// Silhouette
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "org.specs2" %% "specs2-junit" % "2.4.17" % "test",
  "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "com.mohiva" %% "play-silhouette" % "2.0.1" withSources(),
  "com.mohiva" %% "play-silhouette-testkit" % "2.0.1" % "test"
)
