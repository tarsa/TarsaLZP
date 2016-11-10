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

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

case class MainModel(
  options: Options,
  chosenFileOpt: Option[dom.File],
  chunkSize: Int,
  currentTask: ProcessingTask)

sealed trait ProcessingTask {
  def mode: ProcessingMode
}

sealed abstract class CodingInProgress[CoderType](
  val mode: WithCodingMode[CoderType]
) extends ProcessingTask {
  def coder: CoderType

  def inputBuffer: ArrayBuffer

  def processedSymbols: Int

  def outputStream: Streams.ChunksArrayOutputStream

  def startTime: js.Date

  def accumulateProgress(processedSymbols: Int): CodingInProgress[CoderType]
}

case class EncodingInProgress(
  coder: Encoder,
  inputBuffer: ArrayBuffer,
  processedSymbols: Int,
  outputStream: Streams.ChunksArrayOutputStream,
  startTime: js.Date
) extends CodingInProgress(EncodingMode) {
  override def accumulateProgress(processedSymbols: Int) =
    copy(processedSymbols = this.processedSymbols + processedSymbols)
}

case class DecodingInProgress(
  coder: Decoder,
  inputBuffer: ArrayBuffer,
  processedSymbols: Int,
  outputStream: Streams.ChunksArrayOutputStream,
  startTime: js.Date
) extends CodingInProgress(DecodingMode) {
  override def accumulateProgress(processedSymbols: Int) =
    copy(processedSymbols = this.processedSymbols + processedSymbols)
}

case class IdleState(
  mode: ProcessingMode,
  inputBufferOpt: Option[ArrayBuffer],
  codingResultOpt: Option[CodingResult],
  loadingInProgress: Boolean
) extends ProcessingTask

case class CodingResult(
  mode: WithCodingMode[_],
  resultBlob: dom.Blob,
  startTime: js.Date,
  endTime: js.Date,
  totalSymbols: Int)

sealed trait ProcessingMode

sealed trait WithoutCodingMode extends ProcessingMode

case object ShowOptions extends WithoutCodingMode

sealed abstract class WithCodingMode[CoderType] extends ProcessingMode

case object EncodingMode extends WithCodingMode[Encoder]

case object DecodingMode extends WithCodingMode[Decoder]
