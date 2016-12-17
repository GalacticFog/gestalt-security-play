name := """gestalt-security-play"""

organization := "com.galacticfog"

version := "3.0.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-unchecked", "-deprecation", "-feature",
  "-language:postfixOps", "-language:implicitConversions"
)

resolvers ++= Seq(
  "gestalt-snapshots" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "gestalt-releases" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-releases-local",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
)

publishTo <<= version { (v: String) =>
  val ao = "https://galacticfog.artifactoryonline.com/galacticfog/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("publish-gf-snapshots" at ao + "libs-snapshots-local;build.timestamp=" + new java.util.Date().getTime)
  else
    Some("publish-gf-releases"  at ao + "libs-releases-local")
}

isSnapshot := true

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// Adds project name to sbt prompt
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

libraryDependencies ++= Seq(
  "com.galacticfog" %% "gestalt-security-sdk-scala" % "2.3.0-SNAPSHOT",
  "com.mohiva" %% "play-silhouette" 				  % "3.0.5",
  "com.mohiva" %% "play-silhouette-testkit" 		  % "3.0.5",
  "com.typesafe.play" % "play-json_2.11" 			  % "2.4.3",
  "com.typesafe.play" % "play-test_2.11" 			  % "2.4.3"  % "test",
  "de.leanovate.play-mockws" % "play-mockws_2.11"     % "2.4.2"  % "test",
  "org.specs2" %% "specs2-core" 				      % "3.8.3"  % "test",
  "org.specs2" %% "specs2-junit"				      % "3.8.3"  % "test",
  "org.specs2" %% "specs2-mock" 				      % "3.8.3"  % "test",
  "org.specs2" %% "specs2-matcher-extra" 		      % "3.8.3"  % "test"
)


