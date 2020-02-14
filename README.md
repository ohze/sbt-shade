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
import sbtassembly.shadeplugin.ShadePluginUtils._

lazy val `core-io-deps` = project
  .settings(
      libraryDependencies := coreIoShadedDeps,
      publish / skip := true,
      // https://www.scala-sbt.org/1.x/docs/Howto-Classpaths.html#Use+packaged+jars+on+classpaths+instead+of+class+directories
      // exportJars so in dependent projects, we can compute assemblyExcludedJars based on this Project / artifactPath
      exportJars := true
    )

val coreIoAssemblySettings = commonAssemblySettings ++ inTask(assembly)(
  Seq(
    // shade only core-io-deps and selfJar (core-io)
    assemblyExcludedJars := {
      val cp           = fullClasspath.value
      val depJar       = (`core-io-deps` / assembly / assemblyOutputPath).value
      val selfJar      = (Compile / packageBin / artifactPath).value
      val includedJars = Set(depJar, selfJar)
      cp.filterNot { entry =>
        includedJars contains entry.data
      }
    }
  )
)

lazy val `core-io` = project
  .settings(coreIoAssemblySettings: _*)
  .enableAssemblyPublish()
  .settings(
    libraryDependencies ++= coreIoDeps,
    exportJars := true,
    Compile / unmanagedJars += {
      (`core-io-deps` / assembly).value
      (`core-io-deps` / assembly / assemblyOutputPath).value
    }
  )

lazy val `scala-implicits` = project
  .disablePlugins(AssemblyPlugin)
  .settings(
    libraryDependencies ++= scalaImplicitsDeps,
    exportJars := true,
    publish / skip := true
  )
  .dependsOn(`core-io`)

val scalaClientAssemblySettings = commonAssemblySettings ++ inTask(assembly)(
  Seq(
    // shade scala-java8-compat, scala-implicits and selfJar (scala-client)
    assemblyExcludedJars := ...
  )
)

lazy val `scala-client` = project
  .settings(scalaModuleSettings ++ scalaClientAssemblySettings: _*)
  .enableAssemblyPublish()
  .settings(
    libraryDependencies ++= scalaClientDeps
  )
  .dependsOn(`core-io`, `scala-implicits`)
  .removePomDependsOn(`scala-implicits`)
  .removePomDependsOn(scalaJava8Compat)
```
*Note the use of* `enableAssemblyPublish` and `removePomDependsOn` which is defined in [ShadePluginUtils.ShadeProjectOps](src/main/scala/sbtassembly/shadeplugin/ShadePluginUtils.scala)

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
+ Set IntelliJ using scalafmt code formatter
+ sbt
```sbt
+scripted
```
+ publish:
https://www.scala-sbt.org/1.x/docs/Bintray-For-Plugins.html

## changelog
#### 0.1.3
+ Add `Project.removePomDependsOn(moduleIDs: ModuleID*)`
