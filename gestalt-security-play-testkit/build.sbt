import Dependencies._

name := """gestalt-security-play-testkit"""

libraryDependencies ++= Seq(
  Library.Mohiva.silhouetteTestkit,
  // 
  Library.Play.specs2          % Test,
  Library.Specs2.matcherExtra  % Test,
  Library.mockito              % Test,
  Library.akkaTestkit          % Test
)

enablePlugins(PlayScala)
