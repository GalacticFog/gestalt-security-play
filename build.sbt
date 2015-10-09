name := """gestalt-security-play"""

organization := "com.galacticfog"

version := "1.1.3-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/")

credentials ++= {
  (for {
    realm <- sys.env.get("GESTALT_RESOLVER_REALM")
    username <- sys.env.get("GESTALT_RESOLVER_USERNAME")
    resolverUrlStr <- sys.env.get("GESTALT_RESOLVER_URL")
    resolverUrl <- scala.util.Try{url(resolverUrlStr)}.toOption
    password <- sys.env.get("GESTALT_RESOLVER_PASSWORD")
  } yield {
    Seq(Credentials(realm, resolverUrl.getHost, username, password))
  }) getOrElse(Seq())
}

resolvers ++= {
  sys.env.get("GESTALT_RESOLVER_URL") map {
    url => Seq("gestalt-resolver" at url)
  } getOrElse(Seq())
}

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }


libraryDependencies ++= Seq(
  "com.galacticfog" %% "gestalt-security-sdk-scala" % "0.1.2" withSources()
)

// ----------------------------------------------------------------------------
// Silhouette
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "org.specs2" %% "specs2-junit" % "2.4.17" % "test",
  "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "com.mohiva" %% "play-silhouette" % "2.0",
  "com.mohiva" %% "play-silhouette-testkit" % "2.0" % "test"
)
