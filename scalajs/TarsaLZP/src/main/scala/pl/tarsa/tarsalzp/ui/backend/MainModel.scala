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

import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.engine.{Decoder, Encoder}
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.prelude.Streams
import pl.tarsa.tarsalzp.prelude.Streams.ArrayInputStream

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

case class MainModel(
  viewData: ViewData,
  processingData: ProcessingData)


case class ViewData(
  options: Options,
  chosenFileOpt: Option[dom.File],
  chunkSize: Int,
  taskViewData: ProcessingTaskViewData)

sealed trait ProcessingTaskViewData {
  def mode: ProcessingMode
}

case class IdleStateViewData(
  mode: ProcessingMode,
  inputArrayOpt: Option[Uint8Array],
  codingResultOpt: Option[CodingResult],
  loadingInProgress: Boolean
) extends ProcessingTaskViewData

case class CodingResult(
  mode: WithCodingMode,
  totalSymbols: Int,
  compressedSize: Int,
  startTime: js.Date,
  endTime: js.Date,
  codingTimeline: Seq[ChunkCodingMeasurement],
  resultBlob: dom.Blob)

case class CodingInProgressViewData(
  mode: WithCodingMode,
  inputBufferLength: Int,
  inputStreamPosition: Int,
  outputStreamPosition: Int,
  startTime: js.Date,
  codingTimeline: Seq[ChunkCodingMeasurement]
) extends ProcessingTaskViewData


sealed trait ProcessingData

object IdleStateProcessingData extends ProcessingData

sealed trait CodingInProgressProcessingData extends ProcessingData {
  def inputStream: ArrayInputStream

  def outputStream: Streams.ChunksArrayOutputStream
}

class EncodingProcessingData(
  val encoder: Encoder,
  val inputStream: ArrayInputStream,
  val outputStream: Streams.ChunksArrayOutputStream
) extends CodingInProgressProcessingData

class DecodingProcessingData(
  val decoder: Decoder,
  val inputStream: ArrayInputStream,
  val outputStream: Streams.ChunksArrayOutputStream
) extends CodingInProgressProcessingData


case class ChunkCodingMeasurement(
  startTime: js.Date,
  endTime: js.Date,
  symbolsNumber: Int,
  compressedSize: Int)


sealed trait CombinedData

object CombinedData {
  type CodingInProgressCombinedData =
  (CodingInProgressViewData, CodingInProgressProcessingData)
}


sealed trait ProcessingMode

sealed trait WithoutCodingMode extends ProcessingMode

case object ShowOptions extends WithoutCodingMode

sealed abstract class WithCodingMode extends ProcessingMode

case object EncodingMode extends WithCodingMode

case object DecodingMode extends WithCodingMode
