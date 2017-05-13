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
package pl.tarsa.tarsalzp.ui.backend

import akka.actor.ActorRef
import bindings.eligrey.FileSaver
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.CompressionActor.ProcessRequest
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.prelude.WrappedTypedArray
import pl.tarsa.tarsalzp.ui.backend.MainAction._
import pl.tarsa.tarsalzp.ui.backend.MainModel.{
  CodingInProgressViewData,
  CodingResult,
  IdleStateViewData
}
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.{
  DecodingMode,
  EncodingMode,
  ShowOptions,
  WithCodingMode
}

import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

class MainActionHandler[M](modelRW: ModelRW[M, MainModel],
    compressionActor: ActorRef)
    extends ActionHandler(modelRW) {

  type IdleStateActionHandler =
    PartialFunction[(IdleStateAction, IdleStateViewData), ActionResult[M]]

  type CodingInProgressActionHandler = PartialFunction[
      (CodingInProgressAction, CodingInProgressViewData), ActionResult[M]]

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {
    val liftedIdleStateActionHandler =
      idleStateActionHandler.lift
    val liftedCodingInProgressActionHandler =
      codingInProgressActionHandler.lift
    val liftedHandler = (action: Any) => {
      (action, value.taskViewData) match {
        case (action: CodingInProgressAction,
            viewData: CodingInProgressViewData) =>
          liftedCodingInProgressActionHandler(action, viewData)
        case (action: IdleStateAction, viewData: IdleStateViewData) =>
          liftedIdleStateActionHandler(action, viewData)
        case (action: MainAction, _) =>
          println(
              s"Unhandled action $action. Current view data type: " +
                s"${value.taskViewData.getClass}")
          None
        case _ =>
          None
      }
    }
    Function.unlift(liftedHandler)
  }

  private def codingInProgressActionHandler: CodingInProgressActionHandler = {
    case (ChunkProcessed(measurements), viewData) =>
      import viewData._
      val (inputAdvance, outputAdvance) =
        mode match {
          case EncodingMode =>
            (measurements.symbolsNumber, measurements.compressedSize)
          case DecodingMode =>
            (measurements.compressedSize, measurements.symbolsNumber)
        }
      val newViewData = viewData.copy(
        inputStreamPosition = viewData.inputStreamPosition + inputAdvance,
        outputStreamPosition = viewData.outputStreamPosition + outputAdvance,
        codingTimeline = viewData.codingTimeline :+ measurements
      )
      updated(value.copy(taskViewData = newViewData))
    case (ProcessingFinished(endTime, resultBlob), viewData) =>
      val idleStateViewData =
        MainActionHandler.processingFinished(viewData, endTime, resultBlob)
      updated(value.copy(taskViewData = idleStateViewData))
  }

  private def idleStateActionHandler: IdleStateActionHandler = {
    case (UpdateOptions(updater), _) =>
      updated(value.copy(options = updater.run(value.options)))
    case (ChangeChunkSize(newChunkSize), _) =>
      updated(value.copy(chunkSize = newChunkSize))
    case (ChangedMode(newMode), idleStateViewData) =>
      val newModel =
        value.copy(taskViewData = idleStateViewData.copy(mode = newMode))
      updated(newModel)
    case (SelectedFile(fileOpt), _) =>
      println(s"fileOpt = $fileOpt")
      updated(value.copy(chosenFileOpt = fileOpt))
    case (LoadFile, idleStateViewData) =>
      val chosenFile = value.chosenFileOpt.get
      val newModel = value.copy(
          taskViewData = idleStateViewData.copy(loadingInProgress = true))
      updated(newModel, MainActionHandler.loadFileContents(chosenFile))
    case (LoadingFinished(inputBufferOpt), idleStateViewData) =>
      val newModel = value.copy(
          taskViewData = idleStateViewData.copy(
              wrappedInputOpt = inputBufferOpt.map(buffer =>
                  new WrappedTypedArray(new js.typedarray.Uint8Array(buffer))),
              loadingInProgress = false))
      updated(newModel)
    case (StartProcessing, idleStateViewData) =>
      val newViewData = MainActionHandler
        .startProcessingData(idleStateViewData.mode,
          idleStateViewData.wrappedInputOpt.get, value.options,
          value.chunkSize, compressionActor)
        .getOrElse(idleStateViewData)
      updated(value.copy(taskViewData = newViewData))
    case (SaveFile, idleStateViewData) =>
      MainActionHandler.saveResults(
          idleStateViewData.codingResultOpt.get.resultBlob)
      noChange
  }
}

object MainActionHandler {
  def loadFileContents(file: dom.File): Effect = {
    val bufferPromise = Promise[LoadingFinished]()
    val reader = new dom.FileReader()
    reader.onloadstart = (_: dom.ProgressEvent) => {
      println("Loading...")
    }
    reader.onload = (_: dom.UIEvent) => {
      val buffer = reader.result.asInstanceOf[js.typedarray.ArrayBuffer]
      bufferPromise.success(LoadingFinished(Some(buffer)))
      println("Loaded!")
    }
    reader.onloadend = (_: dom.ProgressEvent) => {
      bufferPromise.trySuccess(LoadingFinished(None))
    }
    reader.readAsArrayBuffer(file)
    Effect(bufferPromise.future)
  }

  def saveResults(results: dom.Blob): Unit = {
    FileSaver.saveAs(results, "filename")
    println("Saved!")
  }

  def startProcessingData(mode: ProcessingMode,
      inputWrapper: WrappedTypedArray, options: Options, chunkSize: Int,
      compressionActor: ActorRef): Option[CodingInProgressViewData] = {
    mode match {
      case withCodingMode: WithCodingMode =>
        compressionActor !
          ProcessRequest(withCodingMode, inputWrapper.array, options,
            chunkSize)
        val startTime = new js.Date
        val viewData = CodingInProgressViewData(withCodingMode, inputWrapper,
          inputWrapper.array.length, 0, 0, startTime, Nil)
        Some(viewData)
      case ShowOptions =>
        compressionActor !
          ProcessRequest(ShowOptions, inputWrapper.array, options, chunkSize)
        None
    }
  }

  def processingFinished(codingInProgressViewData: CodingInProgressViewData,
      endTime: js.Date, resultBlob: dom.Blob): IdleStateViewData = {
    import codingInProgressViewData._
    val (totalSymbols, compressedSize) =
      mode match {
        case EncodingMode =>
          (inputBufferLength, outputStreamPosition)
        case DecodingMode =>
          (outputStreamPosition, inputBufferLength)
      }
    val codingResult = CodingResult(mode, totalSymbols, compressedSize,
      startTime, endTime, codingTimeline, resultBlob)
    IdleStateViewData(mode, Some(wrappedInput), Some(codingResult),
      loadingInProgress = false)
  }
}
