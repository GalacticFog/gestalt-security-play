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

git.baseVersion := "4.1.0"
git.useGitDescribe := true

organization      in ThisBuild := "com.galacticfog"
scalaVersion      in ThisBuild := "2.11.8"
isSnapshot        in ThisBuild := true
publishMavenStyle in ThisBuild := true
credentials       in ThisBuild += Credentials(
  "Artifactory Realm",
  "galacticfog.artifactoryonline.com",
  sys.env("GF_ARTIFACTORY_USER"),
  sys.env("GF_ARTIFACTORY_PWD")
)

lazy val gestaltSecurityPlay = (project in file("gestalt-security-play"))
  .enablePlugins(PlayScala,GitVersioning,GitBranchPrompt)

lazy val gestaltSecurityPlayTestkit = (project in file("gestalt-security-play-testkit"))
  .enablePlugins(PlayScala,GitVersioning,GitBranchPrompt)
  .aggregate(gestaltSecurityPlay)
  .dependsOn(gestaltSecurityPlay)
