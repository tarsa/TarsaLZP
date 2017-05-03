enablePlugins(ScalaJSPlugin)

enablePlugins(WorkbenchPlugin)

name := "TarsaLZP"

version := "0-SNAPSHOT"

scalaVersion := "2.11.11"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

scalaJSUseMainModuleInitializer := true

scalaJSUseMainModuleInitializer in Test := false

libraryDependencies ++= Dependencies.scalajsDependencies.value

jsDependencies ++= Dependencies.jsDependencies.value

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
