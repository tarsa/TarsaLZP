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

import diode.Implicits.runAfterImpl
import diode._
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.engine.{Coder, Decoder, Encoder}
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.prelude.Streams

import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scalajs.bindings.eligrey.FileSaver

class MainActionHandler[M](modelRW: ModelRW[M, MainModel])
  extends ActionHandler(modelRW) {

  type IdleStateActionHandler = PartialFunction[
    (IdleStateAction, IdleState), ActionResult[M]]

  type CodingInProgressActionHandler = PartialFunction[
    (CodingInProgressAction, CodingInProgress[_]), ActionResult[M]]

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {
    val liftedIdleStateActionHandler =
      idleStateActionHandler.lift
    val liftedCodingInProgressActionHandler =
      codingInProgressActionHandler.lift
    val liftedHandler = (action: Any) => {
      (action, value.currentTask) match {
        case (action: CodingInProgressAction, task: CodingInProgress[_]) =>
          liftedCodingInProgressActionHandler(action, task)
        case (action: IdleStateAction, task: IdleState) =>
          liftedIdleStateActionHandler(action, task)
        case _ =>
          None
      }
    }
    Function.unlift(liftedHandler)
  }

  private def codingInProgressActionHandler: CodingInProgressActionHandler = {
    case (ContinueProcessing, codingTask) =>
      effectOnly(MainActionHandler.continueProcessing(codingTask,
        value.chunkSize))
    case (ChunkProcessed(symbols), codingTask) =>
      val newCodingTask = codingTask.accumulateProgress(symbols)
      updated(value.copy(currentTask = newCodingTask))
    case (ProcessingFinished(endTime), codingTask) =>
      val idleTask = MainActionHandler.processingFinished(codingTask, endTime)
      updated(value.copy(currentTask = idleTask))
  }

  private def idleStateActionHandler: IdleStateActionHandler = {
    case (UpdateOptions(modify), _) =>
      updated(value.copy(options = modify(value.options)))
    case (ChangeChunkSize(newChunkSize), _) =>
      updated(value.copy(chunkSize = newChunkSize))
    case (ChangedMode(newMode), idleTask) =>
      updated(value.copy(currentTask = idleTask.copy(mode = newMode)))
    case (SelectedFile(fileOpt), _) =>
      println(s"fileOpt = $fileOpt")
      updated(value.copy(chosenFileOpt = fileOpt))
    case (LoadFile, idleTask) =>
      value.chosenFileOpt.fold(noChange) { chosenFile =>
        updated(
          value.copy(currentTask = idleTask.copy(loadingInProgress = true)),
          MainActionHandler.loadFileContents(chosenFile))
      }
    case (LoadingFinished(inputBufferOpt), idleTask) =>
      updated(value.copy(currentTask = idleTask.copy(
        inputBufferOpt = inputBufferOpt,
        loadingInProgress = false)))
    case (StartProcessing, idleTask) =>
      val (newTaskOpt, action) = MainActionHandler.startProcessingData(
        idleTask.mode, idleTask.inputBufferOpt.get,
        value.options, value.chunkSize)
      updated(value.copy(currentTask = newTaskOpt.getOrElse(idleTask)),
        Effect.action(action))
    case (SaveFile, idleTask) =>
      MainActionHandler.saveResults(idleTask.codingResultOpt.get.resultBlob)
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

  def startProcessingData(mode: ProcessingMode, inputBuffer: ArrayBuffer,
    options: Options, chunkSize: Int): (Option[CodingInProgress[_]], Action) = {
    val inputStream = new Streams.ArrayInputStream(
      new Uint8Array(inputBuffer))
    mode match {
      case EncodingMode =>
        val outputStream = new Streams.ChunksArrayOutputStream
        val startTime = new js.Date
        val encoder = Coder.startEncoder(inputStream, outputStream,
          options)
        val encodingInProgress = EncodingInProgress(encoder,
          inputBuffer, 0, outputStream, startTime)
        (Some(encodingInProgress), ContinueProcessing)
      case DecodingMode =>
        val outputStream = new Streams.ChunksArrayOutputStream
        val startTime = new js.Date
        val decoder = Coder.startDecoder(inputStream, outputStream)
        val decodingInProgress = DecodingInProgress(decoder,
          inputBuffer, 0, outputStream, startTime)
        (Some(decodingInProgress), ContinueProcessing)
      case ShowOptions =>
        Coder.checkHeader(inputStream)
        val options = Coder.getOptions(inputStream)
        dom.window.alert(options.prettyFormat)
        (None, NoAction)
    }
  }

  def saveResults(results: dom.Blob): Unit = {
    FileSaver.saveAs(results, "filename")
    println("Saved!")
  }

  def continueProcessing(codingInProgress: CodingInProgress[_],
    chunkSize: Int): Effect = {
    codingInProgress match {
      case encoding: EncodingInProgress
        if encoding.mode == EncodingMode =>
        continueEncoding(encoding, chunkSize)
      case decoding: DecodingInProgress
        if decoding.mode == DecodingMode =>
        continueDecoding(decoding, chunkSize)
    }
  }

  def continueEncoding(codingTask: CodingInProgress[Encoder],
    chunkSize: Int): Effect = {
    val encodedSymbols = codingTask.coder.encode(chunkSize).toInt
    val nextCodingStep =
      if (encodedSymbols == chunkSize) {
        ContinueProcessing
      } else {
        codingTask.coder.flush()
        ProcessingFinished(new js.Date)
      }
    val updateAction = ChunkProcessed(encodedSymbols)
    Effect.action(nextCodingStep).after(Duration.Zero) +
      Effect.action(updateAction)
  }

  def continueDecoding(codingTask: CodingInProgress[Decoder],
    chunkSize: Int): Effect = {
    val decodedSymbols = codingTask.coder.decode(chunkSize).toInt
    val nextCodingStep =
      if (decodedSymbols == chunkSize) {
        ContinueProcessing
      } else {
        ProcessingFinished(new js.Date)
      }
    val updateAction = ChunkProcessed(decodedSymbols)
    Effect.action(nextCodingStep).after(Duration.Zero) +
      Effect.action(updateAction)
  }

  def processingFinished(codingInProgress: CodingInProgress[_],
    endTime: js.Date): IdleState = {
    import codingInProgress._
    IdleState(
      mode,
      inputBufferOpt = Some(inputBuffer),
      codingResultOpt = Some(CodingResult(
        mode, outputStream.toBlob(), startTime, endTime, processedSymbols)),
      loadingInProgress = false
    )
  }
}
