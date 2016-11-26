import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Dependencies {
  object Versions {
    object ScalaJs {
      // general
      val diode = "1.1.0"
      val scalaCss = "0.5.1"
      val scalaJsD3 = "0.3.3"
      val scalaJsDom = "0.9.1"
      val scalaJsReact = "0.11.3"
      // test only
      val scalaTest = "3.0.0"
    }

    object Js {
      // general
      val react = "15.3.2"
    }
  }

  val scalajsDependencies = {
    import Versions.ScalaJs._
    Def.setting(Seq(
      // production
      "com.github.japgolly.scalacss" %%% "core" % scalaCss,
      "com.github.japgolly.scalacss" %%% "ext-react" % scalaCss,
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % scalaJsReact,
      "me.chrons" %%% "diode" % diode,
      "me.chrons" %%% "diode-react" % diode,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDom,
      "org.singlespaced" %%% "scalajs-d3" % scalaJsD3,
      // test
      "org.scalatest" %%% "scalatest" % scalaTest % "test"
    ))
  }

  val jsDependencies = {
    import Versions.Js._
    Def.setting(Seq(
      // production
      "org.webjars.bower" % "react" % react / "react-with-addons.js"
        minified "react-with-addons.min.js" commonJSName "React",
      "org.webjars.bower" % "react" % react / "react-dom.js"
        minified "react-dom.min.js" dependsOn "react-with-addons.js"
        commonJSName "ReactDOM",
      ProvidedJS / "eligrey/Blob.js",
      ProvidedJS / "eligrey/FileSaver.js" dependsOn "eligrey/Blob.js",
      // test
      RuntimeDOM % "test"
    ))
  }
}
