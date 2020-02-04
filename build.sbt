lazy val sbtShade = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.sandinh",
    name := "sbt-shade",
    version := "0.1.0",
    description := "Resource transformers for sbt-assembly plugin",
    organizationName := "Sân Đình",
    organizationHomepage := Some(url("http://sandinh.com")),
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
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
    Test / packageBin / publishArtifact := false,
    Test / packageDoc / publishArtifact := false
  )
