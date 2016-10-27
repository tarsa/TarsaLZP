import com.lihaoyi.workbench.Plugin._

enablePlugins(ScalaJSPlugin)

name := "TarsaLZP"

version := "0-SNAPSHOT"

scalaVersion := "2.11.8"

workbenchSettings

bootSnippet :=
  "pl.tarsa.tarsalzp.TarsaLZP().main(document.getElementById('mainDiv'));"

refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile)

libraryDependencies ++= Seq(
  // production
  "org.scala-js" %%% "scalajs-dom" % "0.8.2",
  "com.lihaoyi" %%% "scalatags" % "0.5.4",
  //test
  "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
)

skip in packageJSDependencies := false

jsDependencies ++= Seq(
  "org.webjars" % "jquery" % "2.1.4" / "2.1.4/jquery.js",
  ProvidedJS / "eligrey/Blob.js",
  ProvidedJS / "eligrey/FileSaver.js" dependsOn "eligrey/Blob.js",
  RuntimeDOM
)
