package sbtassembly.shadeplugin

import sbt.{Setting, _}
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{AssemblyPlugin, MergeStrategy}

object ShadePlugin extends AutoPlugin {
  override def requires = AssemblyPlugin
  override def trigger  = allRequirements

  object autoImport {
    type PartialMergeStrategy = PartialFunction[String, MergeStrategy]
    val shadeResourceTransformers = settingKey[Seq[ResourceTransformer]](
      "Map from original path to renamed path. Can use unix path in all os"
    )
  }

  import autoImport._

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
}
