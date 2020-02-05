[![Build Status](https://travis-ci.org/ohze/sbt-shade.svg?branch=master)](https://travis-ci.org/ohze/sbt-shade)
# sbt-shade
Resource transformers for sbt-assembly plugin

## usage

#### Install
Add to `project/plugins.sbt`
```sbt
addSbtPlugin("com.eed3si9n" % "sbt-assembly"  % "0.14.10")
addSbtPlugin("com.sandinh"  % "sbt-shade"     % "0.1.0")
```

#### Shade
For example, to shade the following dependencies:
```sbt
netty-tcnative-boringssl-static
jackson-core
jackson-databind
jackson-module-afterburner
```
you need add to `build.sbt`
```sbt
import sbtassembly.shadeplugin.ResourceTransformer.{Rename, Discard}

val myAssemblySettings = inTask(assembly)(
  Seq(
    assemblyShadeRules := Seq(
      "io.netty",
      "com.fasterxml.jackson"
    ).map { p =>
       ShadeRule.rename(s"$p.**" -> "com.couchbase.client.core.deps.@0").inAll
     },
    assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
      // https://stackoverflow.com/a/55557287/457612
      case "module-info.class" => MergeStrategy.discard
      case x                   => assemblyMergeStrategy.value(x)
    },
    shadeResourceTransformers ++= Seq(
      Rename(
        "libnetty_tcnative_linux_x86_64.so"   -> "libcom_couchbase_client_core_deps_netty_tcnative_linux_x86_64.so",
        "libnetty_tcnative_osx_x86_64.jnilib" -> "libcom_couchbase_client_core_deps_netty_tcnative_osx_x86_64.jnilib",
        "netty_tcnative_windows_x86_64.dll"   -> "com_couchbase_client_core_deps_netty_tcnative_windows_x86_64.dll"
      ).inDir("META-INF/native"),
      Discard(
        "com.fasterxml.jackson.core.JsonFactory",
        "com.fasterxml.jackson.core.ObjectCodec",
        "com.fasterxml.jackson.databind.Module"
      ).inDir("META-INF/services")
    )
  )  
)

val otherSettings = ...

lazy val `core-io` = project
  .settings(myAssemblySettings ++ otherSettings: _*)
```
*Note the use of* `shadeResourceTransformers ++= Seq(Rename(..), Discard(..)`

#### Include the shaded dependencies into your library for publishing
For example, `com.couchbase.client:core-io` lib want to shade the libraries above into `core-io-<version>.jar`
But keep other libs as normal dependencies (not shade):

```sbt
val coreIoDeps = DependencyTransformer(
  shadedDeps = Seq(netty, jackson ...),
  notShadedDeps = Seq(
    "io.projectreactor" % "reactor-core" % V.reactor
    "org.slf4j"         % "slf4j-api"    % V.slf4j % Optional
  )
)
lazy val `core-io` = project
  .settings(myAssemblySettings ++ coreIoDeps.settings ++ otherSettings: _*)
  .settings(
    addArtifact(artifact in (Compile, assembly), assembly),
  )
```
*Note the use of* `coreIoDeps.settings` which is defined in [DependencyTransformer.scala](src/main/scala/sbtassembly/shadeplugin/DependencyTransformer.scala)
```scala
  def settings = Seq(
    // normally add `shadedDeps` to libraryDependencies
    // but map runtime-like deps in `notShadedDeps` to "provided" scope so that sbt-assembly will not shade those deps
    // see https://github.com/sbt/sbt-assembly#excluding-jars-and-files
    // (runtime-like deps are the dep with no scope or in scopes: Compile, Runtime, Optional, Default)
    libraryDependencies ++= shadedDeps ++ notShadedDepsToProvided,
    // post process the pom <dependencies> xml node for publishing
    // shadedDeps are removed
    // notShadedDeps are change back to the desired scopes
    pomPostProcess := changePomDependencies
  )
```

##### Note for java only projects
Add the following settings:
```sbt
project.settings(
  autoScalaLibrary := false, // exclude scala-library from dependencies
  crossPaths := false        // drop off Scala suffix from artifact names and publish path
)
```

## dev guide
+ clone
+ using IntelliJ
+ Set IntelliJ using scalafmt code formater
+ sbt
```sbt
+scripted
```
+ publish:
https://www.scala-sbt.org/1.x/docs/Bintray-For-Plugins.html
