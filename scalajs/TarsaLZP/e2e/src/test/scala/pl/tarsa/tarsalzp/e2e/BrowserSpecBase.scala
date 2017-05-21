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
package pl.tarsa.tarsalzp.e2e

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.WebBrowser
import org.scalatest.{FlatSpec, MustMatchers}
import pl.tarsa.tarsalzp.e2e.BrowserSpecBase._

abstract class BrowserSpecBase
    extends FlatSpec
    with MustMatchers
    with Eventually
    with WebBrowser {
  BrowserSpecBase

  def withBrowserFixture[T](testBody: BrowserFixture => T): T = {
    val fixture = makeBrowserFixture
    try {
      testBody(fixture)
    } finally {
      fixture.cleanup()
    }
  }

  def byClassName(name: String)(implicit driver: WebDriver): Element = {
    find(className(name)).getOrElse(
        fail(s"Element with class name: $name not found"))
  }
}

object BrowserSpecBase {
  System.setProperty("webdriver.gecko.driver",
    System.getProperty("user.home") + "/devel/geckodriver")

  trait BrowserFixture {
    implicit def webDriver: WebDriver
    def downloadPath: Path
  }

  trait BrowserFixtureWithCleanup extends BrowserFixture {
    def cleanup(): Unit
  }

  def makeBrowserFixture: BrowserFixtureWithCleanup = {
    new BrowserFixtureWithCleanup {
      override val downloadPath: Path =
        Files.createTempDirectory("TarsaLZP-e2e_")

      override implicit val webDriver: WebDriver = {
        def firefoxProfile: FirefoxProfile = {
          val profile = new FirefoxProfile()
          profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
            "example/binary")
          profile.setPreference("browser.download.dir",
            downloadPath.toAbsolutePath.toString)
          profile.setPreference("browser.download.folderList", 2)
          profile
        }
        new FirefoxDriver(firefoxProfile)
      }

      override def cleanup(): Unit = {
        deleteRecursively(downloadPath)
        webDriver.close()
      }
    }
  }

  private def deleteRecursively(root: Path) = {
    val visitor = new SimpleFileVisitor[Path] {
      override def postVisitDirectory(dir: Path,
          exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path,
          attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }
    }
    Files.walkFileTree(root, visitor)
  }
}
