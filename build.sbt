lazy val sbtShade = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.sandinh",
    name := "sbt-shade",
    version := "0.1.0",
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
    // Need publish (even if empty) javadoc as required by sonatype
    // https://github.com/xerial/sbt-sonatype/issues/30#issuecomment-215273906
    Compile / packageDoc / publishArtifact := true,
    Test / publishArtifact := false
  )
  .settings(
    bintraySettings ++ otherSettings: _*
  )

val bintraySettings = Seq(
  publishMavenStyle := false,
  bintrayOrganization := Some("ohze"),
  bintrayRepository := "sbt-plugins"
)

val otherSettings = Seq(
  description := "Resource transformers for sbt-assembly plugin",
  organizationName := "Sân Đình",
  organizationHomepage := Some(url("https://sandinh.com")),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer("ohze", "Bùi Việt Thành", "thanhbv@sandinh.net", url("https://sandinh.com"))
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ohze/sbt-shade"),
      "scm:git:git://github.com/ohze/sbt-shade"
    )
  )
)
