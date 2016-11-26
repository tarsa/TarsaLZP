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
import pl.tarsa.tarsalzp.compression.engine.Coder
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.prelude.Streams
import pl.tarsa.tarsalzp.ui.backend.CombinedData._

import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scalajs.bindings.eligrey.FileSaver

class MainActionHandler[M](modelRW: ModelRW[M, MainModel])
  extends ActionHandler(modelRW) {

  type IdleStateActionHandler = PartialFunction[
    (IdleStateAction, IdleStateViewData), ActionResult[M]]

  type CodingInProgressActionHandler = PartialFunction[
    (CodingInProgressAction, CodingInProgressCombinedData), ActionResult[M]]

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {
    val liftedIdleStateActionHandler =
      idleStateActionHandler.lift
    val liftedCodingInProgressActionHandler =
      codingInProgressActionHandler.lift
    val liftedHandler = (action: Any) => {
      (action, value.viewData.taskViewData, value.processingData) match {
        case (action: CodingInProgressAction,
        viewData: CodingInProgressViewData,
        processingData: CodingInProgressProcessingData) =>
          val combinedData =
            new CodingInProgressCombinedData(viewData, processingData)
          liftedCodingInProgressActionHandler(action, combinedData)
        case (action: IdleStateAction, viewData: IdleStateViewData,
        IdleStateProcessingData) =>
          liftedIdleStateActionHandler(action, viewData)
        case (action: MainAction, _, _) =>
          println(s"Unhandled action $action. Current data types: " +
            s"view - ${value.viewData.getClass}, " +
            s"processing - ${value.processingData.getClass}")
          None
        case _ =>
          None
      }
    }
    Function.unlift(liftedHandler)
  }

  private def codingInProgressActionHandler: CodingInProgressActionHandler = {
    case (ContinueProcessing, combinedData) =>
      effectOnly(MainActionHandler.continueProcessing(combinedData._2,
        value.viewData.chunkSize))
    case (ChunkProcessed(measurements), combinedData) =>
      val oldViewData = combinedData._1
      import oldViewData._
      val (inputAdvance, outputAdvance) =
        mode match {
          case EncodingMode =>
            (measurements.symbolsNumber, measurements.compressedSize)
          case DecodingMode =>
            (measurements.compressedSize, measurements.symbolsNumber)
        }
      val newViewData = oldViewData.copy(
        inputStreamPosition = oldViewData.inputStreamPosition + inputAdvance,
        outputStreamPosition = oldViewData.outputStreamPosition + outputAdvance,
        codingTimeline = oldViewData.codingTimeline :+ measurements)
      updated(value.copy(viewData =
        value.viewData.copy(taskViewData = newViewData)))
    case (ProcessingFinished(endTime), combinedData) =>
      val idleStateViewData =
        MainActionHandler.processingFinished(combinedData, endTime)
      updated(value.copy(
        viewData = value.viewData.copy(taskViewData = idleStateViewData),
        processingData = IdleStateProcessingData))
  }

  private def idleStateActionHandler: IdleStateActionHandler = {
    case (UpdateOptions(modify), _) =>
      updated(transformView(viewData =>
        viewData.copy(options = modify(viewData.options))))
    case (ChangeChunkSize(newChunkSize), _) =>
      updated(transformView(_.copy(chunkSize = newChunkSize)))
    case (ChangedMode(newMode), idleStateViewData) =>
      updated(transformView(_.copy(taskViewData =
        idleStateViewData.copy(mode = newMode))))
    case (SelectedFile(fileOpt), _) =>
      println(s"fileOpt = $fileOpt")
      updated(transformView(_.copy(chosenFileOpt = fileOpt)))
    case (LoadFile, idleStateViewData) =>
      val chosenFile = value.viewData.chosenFileOpt.get
      updated(
        transformView(_.copy(taskViewData =
          idleStateViewData.copy(loadingInProgress = true))),
        MainActionHandler.loadFileContents(chosenFile))
    case (LoadingFinished(inputBufferOpt), idleStateViewData) =>
      updated(transformView(_.copy(taskViewData = idleStateViewData.copy(
        inputArrayOpt = inputBufferOpt.map(buffer => new Uint8Array(buffer)),
        loadingInProgress = false))))
    case (StartProcessing, idleStateViewData) =>
      val (newTaskOpt, action) = MainActionHandler.startProcessingData(
        idleStateViewData.mode, idleStateViewData.inputArrayOpt.get,
        value.viewData.options, value.viewData.chunkSize)
      val (newViewData, newProcessingData) = newTaskOpt.getOrElse(
        (idleStateViewData, IdleStateProcessingData))
      val newModel = value.copy(
        viewData = value.viewData.copy(taskViewData = newViewData),
        processingData = newProcessingData)
      updated(newModel, Effect.action(action))
    case (SaveFile, idleStateViewData) =>
      MainActionHandler.saveResults(
        idleStateViewData.codingResultOpt.get.resultBlob)
      noChange
  }

  private def transformView(transform: ViewData => ViewData): MainModel =
    value.copy(viewData = transform(value.viewData))
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

  def startProcessingData(
    mode: ProcessingMode, inputArray: Uint8Array, options: Options,
    chunkSize: Int): (Option[CodingInProgressCombinedData], Action) = {
    val inputStream = new Streams.ArrayInputStream(inputArray)
    mode match {
      case EncodingMode =>
        val outputStream = new Streams.ChunksArrayOutputStream
        val startTime = new js.Date
        val encoder = Coder.startEncoder(inputStream, outputStream,
          options)
        val encodingInProgress = new CodingInProgressCombinedData(
          CodingInProgressViewData(EncodingMode, inputArray.length, 0, 0,
            startTime, Nil),
          new EncodingProcessingData(encoder, inputStream, outputStream))
        (Some(encodingInProgress), ContinueProcessing)
      case DecodingMode =>
        val outputStream = new Streams.ChunksArrayOutputStream
        val startTime = new js.Date
        val decoder = Coder.startDecoder(inputStream, outputStream)
        val decodingInProgress = new CodingInProgressCombinedData(
          CodingInProgressViewData(DecodingMode, inputArray.length, 0, 0,
            startTime, Nil),
          new DecodingProcessingData(decoder, inputStream, outputStream))
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

  def continueProcessing(processingData: CodingInProgressProcessingData,
    chunkSize: Int): Effect = {
    val startTime = new js.Date
    val (symbolsNumber, compressedSize, nextCodingStep) =
      processingData match {
        case encoding: EncodingProcessingData =>
          import encoding._
          val compressedSizeBefore = outputStream.position
          val encodedSymbols = encoder.encode(chunkSize)
          val compressedSizeAfter = outputStream.position
          val nextCodingStep =
            if (encodedSymbols == chunkSize) {
              ContinueProcessing
            } else {
              encoder.flush()
              ProcessingFinished(new js.Date)
            }
          (encodedSymbols, compressedSizeAfter - compressedSizeBefore,
            nextCodingStep)
        case decoding: DecodingProcessingData =>
          import decoding._
          val compressedSizeBefore = inputStream.position
          val decodedSymbols = decoder.decode(chunkSize)
          val compressedSizeAfter = inputStream.position
          val nextCodingStep =
            if (decodedSymbols == chunkSize) {
              ContinueProcessing
            } else {
              ProcessingFinished(new js.Date)
            }
          (decodedSymbols, compressedSizeAfter - compressedSizeBefore,
            nextCodingStep)
      }
    val endTime = new js.Date
    val updateAction = ChunkProcessed(ChunkCodingMeasurement(
      startTime, endTime, symbolsNumber, compressedSize))
    Effect.action(nextCodingStep).after(Duration.Zero) +
      Effect.action(updateAction)
  }

  def processingFinished(
    codingInProgressData: CodingInProgressCombinedData,
    endTime: js.Date): IdleStateViewData = {
    val (viewData, processingData) = codingInProgressData
    import processingData._
    import viewData._
    val (totalSymbols, compressedSize) =
      mode match {
        case EncodingMode =>
          (inputBufferLength, outputStreamPosition)
        case DecodingMode =>
          (outputStreamPosition, inputBufferLength)
      }
    val codingResult = CodingResult(mode, totalSymbols, compressedSize,
      startTime, endTime, codingTimeline, processingData.outputStream.toBlob())
    IdleStateViewData(mode, Some(inputStream.array), Some(codingResult),
      loadingInProgress = false
    )
  }
}
