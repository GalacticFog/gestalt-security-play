scalacOptions ++= Seq(
  "-unchecked", "-deprecation", "-feature",
  "-language:postfixOps", "-language:implicitConversions"
)

resolvers in ThisBuild ++= Seq(
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "gestalt-snapshots" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "gestalt-releases" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-releases-local"
)

publishTo in ThisBuild <<= version { (v: String) =>
  val ao = "https://galacticfog.artifactoryonline.com/galacticfog/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("publish-gf-snapshots" at ao + "libs-snapshots-local;build.timestamp=" + new java.util.Date().getTime)
  else
    Some("publish-gf-releases"  at ao + "libs-releases-local")
}

organization      in ThisBuild := "com.galacticfog"
version           in ThisBuild := "3.0.2"
scalaVersion      in ThisBuild := "2.11.8"
isSnapshot        in ThisBuild := true
publishMavenStyle in ThisBuild := true
credentials       in ThisBuild += Credentials(Path.userHome / ".ivy2" / ".credentials")

lazy val gestaltSecurityPlay = (project in file("gestalt-security-play"))
  .enablePlugins(PlayScala)

lazy val gestaltSecurityPlayTestkit = (project in file("gestalt-security-play-testkit"))
  .enablePlugins(PlayScala)
  .aggregate(gestaltSecurityPlay)
  .dependsOn(gestaltSecurityPlay)

