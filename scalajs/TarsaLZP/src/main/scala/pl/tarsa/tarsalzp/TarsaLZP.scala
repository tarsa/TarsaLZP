/*
 * Copyright (C) 2016 Piotr Tarsa ( http://github.com/tarsa )
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
package pl.tarsa.tarsalzp

import java.util.UUID.randomUUID

import eligrey.FileSaver
import org.scalajs.dom
import pl.tarsa.imports.ScalaTagsJsDom._
import pl.tarsa.tarsalzp.engine.Coder
import pl.tarsa.tarsalzp.options.OptionsView
import pl.tarsa.tarsalzp.prelude.Streams

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

@JSExport
object TarsaLZP extends js.JSApp {
  @JSExport
  def main(): Unit = {
    val mainDiv = dom.document.getElementById("mainDiv")
    val fileChooser = <.input(^.`type` := "file").render
    val loadButton = <.input(^.`type` := "button",
      ^.value := "Load contents from file").render
    val processButton = <.input(^.`type` := "button",
      ^.value := "Process data").render
    val saveButton = <.input(^.`type` := "button",
      ^.value := "Save results to file").render

    def setButtonsState(enabled: Boolean) = {
      loadButton.disabled = !enabled
      processButton.disabled = !enabled
      saveButton.disabled = !enabled
    }

    def makeModeSwitch(value: String, description: String,
      checked: Boolean = false) = {
      val input = <.input(^.id := randomUUID().toString, ^.name := "mode",
        ^.`type` := "radio", ^.value := value).render
      val label = <.label(^.`for` := input.id, description).render
      if (checked) {
        input.checked = true
      }
      (input, label)
    }

    val encodeLabelledButton =
      makeModeSwitch("encode", "Encode", checked = true)
    val decodeLabelledButton = makeModeSwitch("decode", "Decode")
    val showOptionsLabelledButton =
      makeModeSwitch("showOptions", "Show options")

    var inputBufferOpt: Option[ArrayBuffer] = None
    var outputStreamOpt: Option[Streams.ChunksArrayOutputStream] = None

    def resetBuffers(event: dom.Event): Unit = {
      inputBufferOpt = None
      outputStreamOpt = None
    }

    def loadFileContents(event: dom.Event) = {
      if (fileChooser.files.length > 0) {
        setButtonsState(false)
        val file = fileChooser.files(0)
        val reader = new dom.FileReader()
        reader.onloadstart = (event: dom.ProgressEvent) => {
          println("Loading...")
        }
        reader.onload = (event: dom.UIEvent) => {
          inputBufferOpt = Some(reader.result.asInstanceOf[ArrayBuffer])
        }
        reader.onloadend = (event: dom.ProgressEvent) => {
          setButtonsState(true)
          println("Loaded!")
        }
        reader.readAsArrayBuffer(file)
      }
    }

    def processData(event: dom.Event) = {
      inputBufferOpt.foreach { inputBuffer =>
        val encodeChecked = encodeLabelledButton._1.checked
        val decodeChecked = decodeLabelledButton._1.checked
        val showOptionsChecked = showOptionsLabelledButton._1.checked
        def b2i(b: Boolean): Int = if (b) 1 else 0
        val exactlyOneChecked = b2i(encodeChecked) + b2i(decodeChecked) +
          b2i(showOptionsChecked) == 1
        val inputStream = new Streams.ArrayInputStream(
          new Uint8Array(inputBuffer))
        if (!exactlyOneChecked) {
          dom.window.alert("Something wrong with radio buttons!")
        } else if (encodeChecked) {
          val startTime = js.Date.now()
          val outputStream = new Streams.ChunksArrayOutputStream
          outputStreamOpt = Some(outputStream)
          Coder.encode(inputStream, outputStream, null, 123456,
            OptionsView.currentOptions)
          outputStream.flush()
          val totalTime = js.Date.now() - startTime
          dom.window.alert(f"Encoding done! Time: $totalTime%.3fms")
        } else if (decodeChecked) {
          val startTime = js.Date.now()
          val outputStream = new Streams.ChunksArrayOutputStream
          outputStreamOpt = Some(outputStream)
          Coder.decode(inputStream, outputStream, null, 123456)
          outputStream.flush()
          val totalTime = js.Date.now() - startTime
          dom.window.alert(f"Decoding done! $totalTime%.3fms")
        } else if (showOptionsChecked) {
          val options = Coder.getOptions(inputStream)
          dom.window.alert(options.prettyFormat)
        }
        println("Processed!")
      }
    }

    def saveResults(event: dom.Event) = {
      outputStreamOpt.foreach { outputStream =>
        val chunks = js.Array[js.Any]()
        outputStream.chunksArray.foreach(rawChunk =>
          chunks.push(rawChunk.truncatedBuffer)
        )
        val blob = new dom.Blob(chunks,
          dom.raw.BlobPropertyBag("{type: 'example/binary'}"))
        FileSaver.saveAs(blob, "filename")
        println("Saved!")
      }
    }

    fileChooser.onchange = resetBuffers _
    loadButton.onclick = loadFileContents _
    processButton.onclick = processData _
    saveButton.onclick = saveResults _

    mainDiv.innerHTML = ""
    mainDiv.appendChild(OptionsView.table)
    Seq(
      encodeLabelledButton._1,
      encodeLabelledButton._2,
      decodeLabelledButton._1,
      decodeLabelledButton._2,
      showOptionsLabelledButton._1,
      showOptionsLabelledButton._2,
      <.br().render,
      fileChooser,
      loadButton,
      processButton,
      saveButton
    ).foreach(mainDiv.appendChild)
  }
}
