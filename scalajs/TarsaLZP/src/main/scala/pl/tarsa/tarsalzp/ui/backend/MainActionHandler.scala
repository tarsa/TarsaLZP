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
package pl.tarsa.tarsalzp.ui.backend

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.engine.Coder
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.prelude.Streams

import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scalajs.bindings.eligrey.FileSaver

class MainActionHandler[M](modelRW: ModelRW[M, MainModel])
  extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {
    case UpdateOptions(modify) =>
      updated(value.copy(options = modify(value.options)))
    case ChangedMode(newMode) =>
      updated(value.copy(mode = newMode))
    case SelectedFile(fileOpt) =>
      println(s"fileOpt = $fileOpt")
      updated(value.copy(chosenFileOpt = fileOpt))
    case LoadFile =>
      value.chosenFileOpt.fold(noChange) { chosenFile =>
        updated(value.copy(busy = true),
          MainActionHandler.loadFileContents(chosenFile))
      }
    case LoadingFinished(inputBufferOpt) =>
      updated(value.copy(busy = false,
        buffers = value.buffers.copy(inputBufferOpt = inputBufferOpt)))
    case ProcessFile =>
      updated(value.copy(buffers = MainActionHandler.processData(
        value.mode, value.options, value.buffers)))
    case SaveFile =>
      MainActionHandler.saveResults(value.buffers)
      noChange
    case unknown =>
      println(s"unknown message: $unknown")
      noChange
  }
}


object MainActionHandler {
  def loadFileContents(file: dom.File): Effect = {
    val bufferPromise = Promise[LoadingFinished]()
    val reader = new dom.FileReader()
    reader.onloadstart = (event: dom.ProgressEvent) => {
      println("Loading...")
    }
    reader.onload = (event: dom.UIEvent) => {
      bufferPromise.success(
        LoadingFinished(Some(reader.result.asInstanceOf[ArrayBuffer])))
      println("Loaded!")
    }
    reader.onloadend = (event: dom.ProgressEvent) => {
      bufferPromise.trySuccess(LoadingFinished(None))
    }
    reader.readAsArrayBuffer(file)
    Effect(bufferPromise.future)
  }

  def processData(mode: ProcessingMode, options: Options,
    buffers: Buffers): Buffers = {
    buffers.inputBufferOpt.fold(buffers) { inputBuffer =>
      val inputStream = new Streams.ArrayInputStream(
        new Uint8Array(inputBuffer))
      val newOutputBufferOpt = mode match {
        case Encode =>
          val startTime = js.Date.now()
          val outputStream = new Streams.ChunksArrayOutputStream
          Coder.encode(inputStream, outputStream, null, 123456, options)
          outputStream.flush()
          val totalTime = js.Date.now() - startTime
          dom.window.alert(f"Encoding done! Time: $totalTime%.3fms")
          Some(outputStream)
        case Decode =>
          val startTime = js.Date.now()
          val outputStream = new Streams.ChunksArrayOutputStream
          Coder.decode(inputStream, outputStream, null, 123456)
          outputStream.flush()
          val totalTime = js.Date.now() - startTime
          dom.window.alert(f"Decoding done! $totalTime%.3fms")
          Some(outputStream)
        case ShowOptions =>
          val options = Coder.getOptions(inputStream)
          dom.window.alert(options.prettyFormat)
          None
      }
      println("Processed!")
      buffers.copy(outputStreamOpt =
        newOutputBufferOpt.orElse(buffers.outputStreamOpt))
    }
  }

  def saveResults(buffers: Buffers): Unit = {
    buffers.outputStreamOpt.foreach { outputStream =>
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
}
