scalacOptions ++= Seq(
  "-unchecked", "-deprecation", "-feature",
  "-language:postfixOps", "-language:implicitConversions"
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

lazy val commonSettings = Seq(
  organization := "com.galacticfog",
  version := "3.0.0",
  scalaVersion := "2.11.8"
)

lazy val gestaltSecurityPlay = (project in file("gestalt-security-play"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    // other settings
  )

lazy val gestaltSecurityPlayTestkit = (project in file("gestalt-security-play-testkit"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    // other settings
  )
  .aggregate(gestaltSecurityPlay)
  .dependsOn(gestaltSecurityPlay)

