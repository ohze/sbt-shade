# sbt-shade
Resource transformers for sbt-assembly plugin

## usage

+ `project/plugins.sbt`
```sbt
addSbtPlugin("com.eed3si9n" % "sbt-assembly"  % "0.14.10")
addSbtPlugin("com.sandinh"  % "sbt-shade"     % "0.1.0")
```

+ `build.sbt`

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
```
