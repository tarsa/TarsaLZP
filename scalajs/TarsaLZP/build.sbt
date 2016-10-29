import com.lihaoyi.workbench.Plugin._

enablePlugins(ScalaJSPlugin)

name := "TarsaLZP"

version := "0-SNAPSHOT"

scalaVersion := "2.11.8"

workbenchSettings

bootSnippet := "alert(\"booting!\")"

persistLauncher := true

persistLauncher in Test := false

refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile)

skip in packageJSDependencies := false

libraryDependencies ++= Dependencies.scalajsDependencies.value

jsDependencies ++= Dependencies.jsDependencies.value
