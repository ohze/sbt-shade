package sbtassembly.shadeplugin

import java.io.File

import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport.assembly
import sbtassembly.shadeplugin.ShadePlugin.autoImport.shadeKeys._

object ShadePluginUtils {
  implicit class ShadeStringOps(val s: String) extends AnyVal {
    def osDependentPath: String = s.split(File.separatorChar).mkString(File.separator)
  }
  implicit class ShadeMapOps[T](val m: Map[T, T]) extends AnyVal {
    def mapKeysAndValues(f: T => T): Map[T, T] = m.map {
      case (k, v) => f(k) -> f(v)
    }
  }
  implicit class ShadeSeqOps[T](val m: Seq[(T, T)]) extends AnyVal {
    def mapKeysAndValues(f: T => T): Seq[(T, T)] = m.map {
      case (k, v) => f(k) -> f(v)
    }
  }

  /** Disable publishing packageBin artifacts to prevent conflicting
    * with assembly / artifact when enableAssemblyPublish
    * @see [[sbt.Classpaths.jvmPublishSettings]] */
  private val shadedJvmPublishSettings: Seq[Setting[_]] = {
    // don't include `packageBin` as in Classpaths.defaultPackageKeys
    val defaultPackageKeys = Seq(packageSrc, packageDoc)
    lazy val defaultPackages: Seq[TaskKey[File]] =
      for (task <- defaultPackageKeys; conf <- Seq(Compile, Test)) yield task in conf
    lazy val defaultArtifactTasks: Seq[TaskKey[File]] = makePom +: defaultPackages

    import Classpaths._
    Seq(
      artifacts := artifactDefs(defaultArtifactTasks).value,
      packagedArtifacts := packaged(defaultArtifactTasks).value
    )
  }

  /** Extension methods for sbt.Project */
  implicit class ShadeProjectOps(val p: Project) extends AnyVal {

    /** add assembly shaded jar as an artifact to be published */
    def enableAssemblyPublish(): Project = p.settings(
      shadedJvmPublishSettings :+
        addArtifact(artifact in (Compile, assembly), assembly): _*
    )

    /** Set pomPostProcess for this `p` project to remove `d` as a dependency xml node in pom.xml when publishing */
    def removePomDependsOn(d: ProjectReference): Project = p.settings(
      pomPostProcess := {
        val groupId    = (organization in d).value
        val artifactId = (name in d).value + (artifactIdSuffix in d).value
        removeDependencyFromPom(artifactNodeMatcher(groupId -> artifactId))
      }
    )

    /** Set pomPostProcess for this `p` project to remove `rules` as dependency xml nodes in pom.xml when publishing
      * usage example: {{{
      *   val zio = "dev.zio" %% "zio" % "1.0.0-RC17"
      *   val otherDep = ..
      *   val p = project
      *     .settings(
      *        libraryDependencies += zio,
      *        .. shade zio, otherDep
      *     )
      *     .removePomDependsOn(zio, otherDep)
      * }}} */
    def removePomDependsOn(moduleIDs: ModuleID*): Project = p.settings(
      pomPostProcess := removeDependencyFromPom(
        ScalaVersion(scalaVersion.value, scalaBinaryVersion.value),
        moduleIDs: _*
      )
    )

    private def removeDependencyFromPom(
        sv: ScalaVersion,
        moduleIDs: ModuleID*
    ): xml.Node => xml.Node = {
      val matcherRules = moduleIDs.map { m =>
        val groupId = m.organization
        val cross   = CrossVersion(m.crossVersion, sv.full, sv.binary)
        val artifactId = cross match {
          case None    => m.name
          case Some(f) => f(m.name)
        }
        groupId -> artifactId
      }

      removeDependencyFromPom(artifactNodeMatcher(matcherRules: _*))
    }

    /** Remove {{{<dependency>}}} nodes from {{{<dependencies>}}} node in pom.xml
      * @param depNodeMatcher if depNodeMatcher(dependency node) == true then the dependency node will be removed */
    private def removeDependencyFromPom(depNodeMatcher: xml.Node => Boolean): xml.Node => xml.Node = {
      case e: xml.Elem =>
        e.copy(child =
          if (e.label == "dependencies") e.child.filterNot(depNodeMatcher)
          else e.child.map(removeDependencyFromPom(depNodeMatcher))
        )
      case x => x
    }

    /** check if a Node is {{{
      * <dependency>
      *   <groupId>{text == groupId in rules}</groupId>
      *   <artifactId>{text == artifactId in rules}</artifactId>
      *   ...
      * }}}
      * @param rules seq of groupId -> artifactId to match
      * */
    private def artifactNodeMatcher(rules: (String, String)*): xml.Node => Boolean = {
      case n: xml.Elem if n.label == "dependency" =>
        rules.contains(
          (n \ "groupId").head.text ->
            (n \ "artifactId").head.text
        )
      case _ => false
    }
  }
}
