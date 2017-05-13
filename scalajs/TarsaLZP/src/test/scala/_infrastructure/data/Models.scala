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
package _infrastructure.data

import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.data.WrappedTypedArray
import pl.tarsa.tarsalzp.ui.backend.MainModel.{
  CodingInProgressViewData,
  IdleStateViewData
}
import pl.tarsa.tarsalzp.ui.backend.{MainModel, ProcessingMode}

import scala.scalajs.js

object Models {
  private val P = Parameters
  private val VD = TaskViewData

  val initial: MainModel =
    MainModel(P.initialOptions, Option.empty[dom.File], P.initialChunkSize,
      VD.initial)

  val optionsUpdated: MainModel =
    initial.copy(options = P.newOptions)

  var chunksSizeChanged: MainModel =
    initial.copy(chunkSize = P.newChunkSize)

  val modeChanged: MainModel =
    initial.copy(taskViewData = VD.modeChanged)

  val fileSelected: MainModel =
    initial.copy(chosenFileOpt = P.newChosenFile)

  val fileLoadingStarted: MainModel =
    fileSelected.copy(taskViewData = VD.fileLoadingStarted)

  val fileLoadingFinished: MainModel =
    fileSelected.copy(taskViewData = VD.fileLoadingFinished)

  val encodingStarted: MainModel =
    fileLoadingFinished.copy(taskViewData = VD.encodingStarted)

  val firstMeasurementReceived: MainModel =
    encodingStarted.copy(taskViewData = VD.firstMeasurementReceived)

  val secondMeasurementReceived: MainModel =
    encodingStarted.copy(taskViewData = VD.secondMeasurementReceived)

  val afterProcessingFinished: MainModel =
    fileLoadingFinished.copy(taskViewData = VD.afterProcessingFinished)

  private object TaskViewData {
    val initial: IdleStateViewData =
      IdleStateViewData(ProcessingMode.EncodingMode, None, None,
        loadingInProgress = false)

    val modeChanged: IdleStateViewData =
      initial.copy(mode = P.newMode)

    val fileLoadingStarted: IdleStateViewData =
      initial.copy(loadingInProgress = true)

    val fileLoadingFinished: IdleStateViewData =
      initial.copy(wrappedInputOpt = Some(P.wrappedInput))

    val encodingStarted: CodingInProgressViewData =
      CodingInProgressViewData(ProcessingMode.EncodingMode, P.wrappedInput,
        P.wrappedInput.array.length, 0, 0, P.encodingStartTime, Seq.empty)

    val firstMeasurementReceived: CodingInProgressViewData =
      encodingStarted.copy(inputStreamPosition = 2, outputStreamPosition = 1,
        codingTimeline = Seq(P.firstMeasurement))

    val secondMeasurementReceived: CodingInProgressViewData =
      encodingStarted.copy(inputStreamPosition = 5, outputStreamPosition = 4,
        codingTimeline = Seq(P.firstMeasurement, P.secondMeasurement))

    private val codingResult = MainModel.CodingResult(
        ProcessingMode.EncodingMode, 3, 4, P.encodingStartTime,
        P.encodingEndTime, Seq(P.firstMeasurement, P.secondMeasurement),
        P.resultingBlob)

    val afterProcessingFinished: IdleStateViewData =
      fileLoadingFinished.copy(codingResultOpt = Some(codingResult))
  }

  object Parameters {
    val newLiteralCoderLimit: Int = 777

    val initialOptions: Options =
      Options.default

    val newOptions: Options =
      Options.default.copy(literalCoderLimit = newLiteralCoderLimit)

    val initialChunkSize: Int = 345

    val newChunkSize: Int = 3456

    val newMode: ProcessingMode =
      ProcessingMode.ShowOptions

    val inputBuffer: js.typedarray.ArrayBuffer =
      new js.typedarray.ArrayBuffer(3)

    val newChosenFile: Option[dom.File] =
      Some(new dom.Blob(js.Array(inputBuffer)).asInstanceOf[dom.File])

    val wrappedInput: WrappedTypedArray =
      new WrappedTypedArray(new js.typedarray.Uint8Array(inputBuffer))

    val encodingStartTime: js.Date =
      new js.Date()

    val firstMeasurement: MainModel.ChunkCodingMeasurement =
      MainModel.ChunkCodingMeasurement(new js.Date(), new js.Date(), 2, 1)

    val secondMeasurement: MainModel.ChunkCodingMeasurement =
      MainModel.ChunkCodingMeasurement(new js.Date(), new js.Date(), 3, 3)

    val encodingEndTime: js.Date =
      new js.Date()

    val resultingBlob: dom.Blob =
      new dom.Blob()
  }
}
