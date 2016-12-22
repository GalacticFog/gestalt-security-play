import Dependencies._

libraryDependencies ++= Seq(
  Library.Play.ws,
  Library.Play.json,
  Library.Gestalt.securitySdk,
  Library.Mohiva.silhouette,
  // 
  Library.Play.specs2         % Test,
  Library.Specs2.matcherExtra % Test
  // Library.mockito % Test,
  // Library.scalaGuice % Test
)

// libraryDependencies ++= Seq(
  // "com.typesafe.play" %% "play-json"                % "2.4.6",
  // "com.typesafe.play" %% "play-test"                % "2.4.6" % Test,
  // "com.typesafe.play" %% "play-specs2"              % "2.4.6" % Test,
  // "com.mohiva" %% "play-silhouette"                 % "3.0.5",
  // "com.mohiva" %% "play-silhouette-testkit"         % "3.0.5",
  // "de.leanovate.play-mockws" %% "play-mockws"       % "2.4.2" % Test,
  // "org.specs2" %% "specs2-core"                     % "3.8.3" % Test,
  // "org.specs2" %% "specs2-junit"                    % "3.8.3" % Test,
  // "org.specs2" %% "specs2-mock"                     % "3.8.3" % Test,
  // "org.specs2" %% "specs2-matcher-extra"            % "3.8.3" % Test
// )
