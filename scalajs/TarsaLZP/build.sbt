import com.lihaoyi.workbench.Plugin._

enablePlugins(ScalaJSPlugin)

name := "TarsaLZP"

version := "0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

workbenchSettings

// why it's needed for refreshing?
bootSnippet := "console.log(\"i'm just refreshing\")"

persistLauncher := true

persistLauncher in Test := false

refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile)

skip in packageJSDependencies := false

libraryDependencies ++= Dependencies.scalajsDependencies.value

jsDependencies ++= Dependencies.jsDependencies.value
