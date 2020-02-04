package sbtassembly.shadeplugin

import sbt.Keys._
import sbt._

import scala.xml.Elem

object DependencyTransformer {

  /** represent a dependency Node
    * @param elem like this: {{{
    * <dependency>
    *   <groupId/>
    *   <artifactId/>
    *   <version/>
    *   <scope/>
    *   <optional/>
    * </dependency>
    * }}} */
  class DepNode private (val elem: xml.Elem) {
    def groupId: String    = (elem \ "groupId").head.text
    def artifactId: String = (elem \ "artifactId").head.text

    /** Check `n` has child {{{<scope>provided</scope>}}} */
    def hasProvidedScope: Boolean = (elem \ "scope").exists(_.text == Provided.name)

    /** @see [[DependencyTransformer.shouldChangeToProvided]] */
    def changeScopeBack(notShadedDeps: Seq[ModuleID]): Elem = {
      if (!hasProvidedScope) elem
      else
        findIn(notShadedDeps) match {
          case None => elem
          case Some(m) =>
            elem.copy(child = elem.child.flatMap {
              case x if x.label == "scope" =>
                m.configurations match {
                  case None                => Seq.empty
                  case Some(Optional.name) => Seq.empty
                  case Some(s)             => <scope>{s}</scope>
                }
              case x => Seq(x)
            })
        }
    }

    def findIn(deps: Seq[ModuleID]): Option[ModuleID] =
      deps.find { m =>
        m.organization == groupId && m.name == artifactId
      }
  }

  object DepNode {
    def unapply(n: xml.Node): Option[DepNode] = n match {
      case n: xml.Elem if n.label == "dependency" => Some(new DepNode(n))
      case _                                      => None
    }
  }

  private def shouldChangeToProvided(m: ModuleID): Boolean = m.configurations match {
    case None    => true
    case Some(c) => Seq(Compile, Runtime, Optional, Default).map(_.name).contains(c)
  }
}

case class DependencyTransformer(
    shadedDeps: Seq[ModuleID],
    notShadedDeps: Seq[ModuleID]
) {
  import DependencyTransformer._

  private def notShadedDepsToProvided = notShadedDeps.map {
    case m if shouldChangeToProvided(m) => m.withConfigurations(None) % Provided
    case m                              => m
  }

  private def changePomDependencies: xml.Node => xml.Node = {
    case elem: xml.Elem =>
      val child = elem.child
      elem.copy(child = if (elem.label == "dependencies") {
        child
          .withFilter {
            // remove shaded dependencies
            case DepNode(d) => d.findIn(shadedDeps).isEmpty
            case _          => true
          }
          .map {
            case DepNode(d) => d.changeScopeBack(notShadedDeps)
            case x          => x
          }
      } else {
        child.map(changePomDependencies)
      })
    case x => x
  }

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
}
