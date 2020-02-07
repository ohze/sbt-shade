package sbtassembly.shadeplugin

import java.io.File

import sbtassembly.shadeplugin.ShadePlugin.autoImport._
import sbtassembly.shadeplugin.ShadePluginUtils._
import sbtassembly.{MappingSet, MergeStrategy}

abstract class ResourceTransformer extends MergeStrategy {

  /** resource paths to transform */
  def resourcePaths: Vector[String]

  def selfStrategy: PartialMergeStrategy = {
    case p if resourcePaths.contains(p) => this
  }

  /** A MappingSet from an empty path File to `resourcePaths`.
    * This mappingSet will be add to assembly / assembledMappings
    * so when assemblyPlugin processing file that match resourcePaths => duplication => trigger MergeStrategy
    * @note we need this because a limit (for now :D) in sbt-assembly that don't provide a way to hook a logic
    * when processing a resource with no conflict occurred */
  // TODO ?should use AssemblyOption.excludedFiles
  def mappingSet: MappingSet = MappingSet(
    None,
    resourcePaths.map(new File("") -> _)
  )
}

object ResourceTransformer {
  class Rename private (_rules: Map[String, String]) extends ResourceTransformer {
    private[this] val rules = _rules.mapKeysAndValues(_.osDependentPath)

    val resourcePaths: Vector[String] = rules.keys.toVector

    def inDir(dir: String): Rename = new Rename(rules.mapKeysAndValues(n => s"$dir/$n"))

    val name = "ResourceTransformer.Rename"

    def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
      if (files.size > 1) MergeStrategy.deduplicate(tempDir, path, files)
      else Right(Seq(files.head -> rules(path)))

    override def notifyThreshold = 1
  }
  object Rename {
    def apply(rules: (String, String)*) = new Rename(rules.toMap)
  }

  class Discard private (_paths: Vector[String]) extends ResourceTransformer {
    val resourcePaths: Vector[String] = _paths.map(_.osDependentPath)

    def inDir(dir: String): Discard = new Discard(resourcePaths.map(n => s"$dir/$n"))

    val name = "ResourceTransformer.Discard"

    def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
      Right(Nil)

    override def notifyThreshold = 1
  }
  object Discard {
    def apply(paths: String*) = new Discard(paths.toVector)
  }
}
