import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Dependencies {
  object Versions {
    object ScalaJs {
      // production
      val scalaJsDom = "0.9.1"
      val scalaTags = "0.6.1"
      // test
      val scalaTest = "3.0.0"
    }

    object Js {
      // production
      val jQuery = "2.1.4"
    }
  }

  val scalajsDependencies = {
    import Versions.ScalaJs._
    Def.setting(Seq(
      // production
      "org.scala-js" %%% "scalajs-dom" % scalaJsDom,
      "com.lihaoyi" %%% "scalatags" % scalaTags,
      // test
      "org.scalatest" %%% "scalatest" % scalaTest % "test"
    ))
  }

  val jsDependencies = {
    import Versions.Js._
    Def.setting(Seq(
      // production
      "org.webjars" % "jquery" % jQuery / s"$jQuery/jquery.js",
      ProvidedJS / "eligrey/Blob.js",
      ProvidedJS / "eligrey/FileSaver.js" dependsOn "eligrey/Blob.js",
      // test
      RuntimeDOM % "test"
    ))
  }
}
