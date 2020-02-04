package sbtassembly.shadeplugin

import java.io.File

object ShadePluginUtils {
  implicit class StringOps(val s: String) extends AnyVal {
    def osDependentPath: String = s.split(File.separatorChar).mkString(File.separator)
  }
  implicit class MapOps[T](val m: Map[T, T]) extends AnyVal {
    def mapKeysAndValues(f: T => T): Map[T, T] = m.map {
      case (k, v) => f(k) -> f(v)
    }
  }
  implicit class SeqOps[T](val m: Seq[(T, T)]) extends AnyVal {
    def mapKeysAndValues(f: T => T): Seq[(T, T)] = m.map {
      case (k, v) => f(k) -> f(v)
    }
  }
}
