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

lazy val commonSettings = Seq(
  organization := "pl.tarsa",
  version := "0-SNAPSHOT",
  scalaVersion := "2.11.11",
  scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature"),
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
)

lazy val locators = (project in file("locators"))
  .settings(commonSettings: _*)
  .settings(name := "TarsaLZP-locators")

lazy val app =
  (project in file("app"))
    .settings(commonSettings: _*)
    .settings(name := "TarsaLZP")
    .settings(
      libraryDependencies ++= Dependencies.scalajsDependencies.value,
      jsDependencies ++= Dependencies.jsDependencies.value
    )
    .dependsOn(locators)

lazy val e2e =
  (project in file("e2e"))
    .settings(commonSettings: _*)
    .settings(name := "TarsaLZP-e2e")
    .dependsOn(locators)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "TarsaLZP-root")
  .aggregate(locators, app, e2e)
  .dependsOn(locators, app, e2e)
