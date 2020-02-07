package sbtassembly.shadeplugin

import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{AssemblyPlugin, MergeStrategy}

object ShadePlugin extends AutoPlugin {
  override def requires = AssemblyPlugin
  override def trigger  = allRequirements

  object autoImport {
    type PartialMergeStrategy = PartialFunction[String, MergeStrategy]
    lazy val shadeResourceTransformers = settingKey[Seq[ResourceTransformer]](
      "Map from original path to renamed path. Can use unix path in all os"
    )

    /** Add shadeKeys to prevent conflict with other sbt keys
      * @see https://www.scala-sbt.org/1.x/docs/Plugins-Best-Practices.html */
    object shadeKeys {
      lazy val artifactIdSuffix = settingKey[String](
        "Util setting. = `_$scalaBinaryVersion` if `crossPaths`, empty otherwise"
      )
    }
  }

  import autoImport._
  import autoImport.shadeKeys._

  override lazy val projectSettings: Seq[Setting[_]] = inTask(assembly)(
    Seq(
      shadeResourceTransformers := Seq.empty,
      assemblyMergeStrategy := { p =>
        shadeResourceTransformers.value
          .foldLeft[PartialMergeStrategy](PartialFunction.empty) {
            case (acc, t) => acc.orElse(t.selfStrategy)
          }
          .applyOrElse(
            p,
            assemblyMergeStrategy.value // oldStrategy
          )
      },
      assembledMappings ++= shadeResourceTransformers.value.map(_.mappingSet)
    )
  )

  override val globalSettings: Seq[Def.Setting[_]] = Seq(
    derive(artifactIdSuffix := {
      if (crossPaths.value) "_" + scalaBinaryVersion.value
      else ""
    })
  )

  /** copy from [[sbt.BuildCommon.derive]]
    * which is used to define scalaBinaryVersion in sbt.Defaults.compileBaseGlobal */
  private[this] def derive[T](s: Setting[T]): Setting[T] =
    Def.derive(s, allowDynamic = true, trigger = _ != streams.key, default = true)
}
