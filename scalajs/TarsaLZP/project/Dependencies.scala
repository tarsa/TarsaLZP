/*
 * Copyright (C) 2016 - 2017 Piotr Tarsa ( http://github.com/tarsa )
 *
 *  This software is provided 'as-is', without any express or implied
 *  warranty.  In no event will the author be held liable for any damages
 *  arising from the use of this software.
 *
 *  Permission is granted to anyone to use this software for any purpose,
 *  including commercial applications, and to alter it and redistribute it
 *  freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *  3. This notice may not be removed or altered from any source distribution.
 *
 */

import org.scalajs.sbtplugin.AbstractJSDep
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  object Versions {
    object ScalaJs {
      // general
      val akkaJs = "1.2.5.0"
      val diode = "1.1.1"
      val scalaCss = "0.5.1"
      val scalaJsD3 = "0.3.3"
      val scalaJsDom = "0.9.1"
      val scalaJsReact = "0.11.3"
      // test only
      val scalaTest = "3.0.1"
    }

    object Js {
      // general
      val react = "15.3.2"
    }
  }

  val scalajsDependencies: Def.Initialize[Seq[ModuleID]] = {
    import Versions.ScalaJs._
    Def.setting(Seq(
      // production
      "com.github.japgolly.scalacss" %%% "core" % scalaCss,
      "com.github.japgolly.scalacss" %%% "ext-react" % scalaCss,
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % scalaJsReact,
      "io.suzaku" %%% "diode" % diode,
      // diode-react sources copied verbatim
      // "io.suzaku" %%% "diode-react" % diode,
      "org.akka-js" %%% "akkajsactor" % akkaJs,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDom,
      "org.singlespaced" %%% "scalajs-d3" % scalaJsD3,
      // test
      "com.github.japgolly.scalajs-react" %%% "test" % scalaJsReact % Test,
      "org.akka-js" %%% "akkajstestkit" % akkaJs % Test,
      "org.scalatest" %%% "scalatest" % scalaTest % Test
    ))
  }

  val jsDependencies: Def.Initialize[Seq[AbstractJSDep]] = {
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
      RuntimeDOM % Test
    ))
  }
}