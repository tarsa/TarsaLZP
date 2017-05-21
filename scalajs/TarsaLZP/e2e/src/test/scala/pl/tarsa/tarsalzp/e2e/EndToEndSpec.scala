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

import java.nio.file.{Files, Path, StandardCopyOption}

import org.apache.commons.io.IOUtils
import org.openqa.selenium.WebDriver
import pl.tarsa.tarsalzp.e2e.BrowserSpecBase.BrowserFixture
import pl.tarsa.tarsalzp.ui.locators.MainLocators

import scala.concurrent.duration._

class EndToEndSpec extends BrowserSpecBase {
  val demoPage = "https://tarsa.github.io/TarsaLZP-demo/"

  behavior of "Application"

  it must "encode" in withBrowserFixture(codingTest("encode", "dec", "enc"))

  it must "decode" in withBrowserFixture(codingTest("decode", "enc", "dec"))

  it must "show options" in withBrowserFixture { fixture =>
    import fixture._
    go to demoPage
    val buttons = new Buttons()
    import buttons._
    click on className(MainLocators.modeSwitch("showOptions"))
    val file = resourceToFile("/pl/tarsa/tarsalzp/e2e/data/example", "enc2")
    fileChooser.underlying.sendKeys(file.toAbsolutePath.toString)
    click on loadButton
    eventually(click on processButton)
    Files.delete(file)
    val popup = eventually(switch to alertBox)
    val message = popup.getText
    popup.accept()
    message mustBe """Options:
                     |Low Context Length: 3
                     |Low Mask Size: 20
                     |High Context Length: 5
                     |High Mask Size: 23
                     |Literal Coder Order: 1
                     |Literal Coder Init: 4
                     |Literal Coder Step: 57
                     |Literal Coder Limit: 30004
                     |""".stripMargin
  }

  private def codingTest(modeName: String, sourceExtension: String,
      targetExtension: String)(fixture: BrowserFixture): Unit = {
    import fixture._
    go to demoPage
    val buttons = new Buttons()
    import buttons._
    click on className(MainLocators.modeSwitch(modeName))
    val file =
      resourceToFile("/pl/tarsa/tarsalzp/e2e/data/example", sourceExtension)
    fileChooser.underlying.sendKeys(file.toAbsolutePath.toString)
    click on loadButton
    eventually(click on processButton)
    Files.delete(file)
    eventually(timeout(2.seconds))(click on saveButton)
    val resultFile = downloadPath.resolve("filename")
    eventually(Files.exists(resultFile) mustBe true)
    val actualContents = Files.readAllBytes(resultFile)
    val expectedContents = IOUtils.toByteArray(
        this.getClass.getResource(
            s"/pl/tarsa/tarsalzp/e2e/data/example.$targetExtension"))
    actualContents mustBe expectedContents
  }

  private def resourceToFile(resourcePathPrefix: String,
      extension: String): Path = {
    val file = Files.createTempFile("TarsaLZP-e2e_", s".$extension")
    Files.copy(
        this.getClass.getResourceAsStream(s"$resourcePathPrefix.$extension"),
        file, StandardCopyOption.REPLACE_EXISTING)
    file
  }

  class Buttons()(implicit webDriver: WebDriver) {
    val fileChooser: Element = byClassName(MainLocators.fileChooser)
    val loadButton: Element = byClassName(MainLocators.loadButton)
    val processButton: Element = byClassName(MainLocators.processButton)
    val saveButton: Element = byClassName(MainLocators.saveButton)
  }
}
