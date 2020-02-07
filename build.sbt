lazy val sbtShade = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.sandinh",
    name := "sbt-shade",
    version := "0.1.2",
    scalacOptions := Seq("-deprecation", "-Xfuture"),
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10"),
    crossScalaVersions := Seq("2.12.10", "2.10.7"),
    scalaVersion := crossScalaVersions.value.head,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.18"
        case "2.12" => "1.3.7"
      }
    },
    Compile / doc / sources := Nil,
    Test / publishArtifact := false
  )
  .settings(infoSettings: _*)

val publishInfoSettings = Seq(
  publishMavenStyle := false,
  bintrayRepository := "sbt-plugins",
  bintrayOrganization in bintray := None,
  bintrayPackageLabels := Seq(
    "sbt",
    "sbt-assembly",
    "sbt-shade",
    "maven-shade-plugin",
    "dependencies"
  )
)

val sandinh = url("https://sandinh.com")

val infoSettings = Seq(
  description := "Resource transformers for sbt-assembly plugin",
  organizationName := "Sân Đình",
  organizationHomepage := Some(sandinh),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer("ohze", "Bùi Việt Thành", "thanhbv@sandinh.net", sandinh)
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ohze/sbt-shade"),
      "scm:git:git://github.com/ohze/sbt-shade"
    )
  )
)
