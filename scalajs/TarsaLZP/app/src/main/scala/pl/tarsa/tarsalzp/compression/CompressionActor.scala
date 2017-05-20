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
package pl.tarsa.tarsalzp.compression

import akka.actor.{Actor, ActorRef, Props}
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.CompressionActor._
import pl.tarsa.tarsalzp.compression.engine.{Coder, Decoder, Encoder}
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.data.Streams
import pl.tarsa.tarsalzp.data.Streams.ArrayInputStream
import pl.tarsa.tarsalzp.system.Clock
import pl.tarsa.tarsalzp.ui.backend.MainAction.{
  ChunkProcessed,
  CodingInProgressAction,
  ProcessingFinished
}
import pl.tarsa.tarsalzp.ui.backend.MainModel.ChunkCodingMeasurement
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.{
  DecodingMode,
  EncodingMode,
  ShowOptions
}

import scala.concurrent.duration.Duration
import scala.scalajs.js

class CompressionActor(
    actionDispatch: CodingInProgressAction => Unit,
    clock: Clock
) extends Actor {
  import CompressionActor.InternalMessages.ContinueProcessing

  var processingDataOpt: Option[ProcessingData] = None

  override def receive: Receive = {
    case ContinueProcessing if processingDataOpt.nonEmpty =>
      val processingData = processingDataOpt.get
      val startTime = clock.currentDate
      val (symbolsNumber, compressedSize, finishingMessageOpt) =
        processingData match {
          case encoding: EncodingProcessingData =>
            import encoding._
            val compressedSizeBefore = outputStream.position
            val encodedSymbols = encoder.encode(chunkSize)
            val compressedSizeAfter = outputStream.position
            val finishingMessageOpt =
              if (encodedSymbols == chunkSize) {
                None
              } else {
                encoder.flush()
                Some(ProcessingFinished(clock.currentDate, outputStream))
              }
            (encodedSymbols, compressedSizeAfter - compressedSizeBefore,
              finishingMessageOpt)
          case decoding: DecodingProcessingData =>
            import decoding._
            val compressedSizeBefore = inputStream.position
            val decodedSymbols = decoder.decode(chunkSize)
            val compressedSizeAfter = inputStream.position
            val finishingMessageOpt =
              if (decodedSymbols == chunkSize) {
                None
              } else {
                Some(ProcessingFinished(clock.currentDate, outputStream))
              }
            (decodedSymbols, compressedSizeAfter - compressedSizeBefore,
              finishingMessageOpt)
        }
      val endTime = clock.currentDate
      val updateAction = ChunkProcessed(ChunkCodingMeasurement(startTime,
          endTime, symbolsNumber, compressedSize))
      actionDispatch(updateAction)
      finishingMessageOpt.fold(continueProcessing()) { finishingMessage =>
        actionDispatch(finishingMessage)
        processingData.requestSenderOpt.foreach(_ ! RequestProcessed)
        processingDataOpt = None
      }
    case ProcessRequest(mode, inputArray, options, chunkSize)
        if processingDataOpt.isEmpty =>
      val inputStream = new Streams.ArrayInputStream(inputArray)
      mode match {
        case ShowOptions =>
          Coder.checkHeader(inputStream)
          val options = Coder.getOptions(inputStream)
          dom.window.alert(options.prettyFormat)
        case EncodingMode =>
          val outputStream = new Streams.ChunksArrayOutputStream
          val encoder = Coder.startEncoder(inputStream, outputStream, options)
          processingDataOpt = Some(new EncodingProcessingData(Option(sender()),
              encoder, inputStream, outputStream, chunkSize))
          continueProcessing()
        case DecodingMode =>
          val outputStream = new Streams.ChunksArrayOutputStream
          val decoder = Coder.startDecoder(inputStream, outputStream)
          processingDataOpt = Some(new DecodingProcessingData(Option(sender()),
              decoder, inputStream, outputStream, chunkSize))
          continueProcessing()
      }
  }

  def continueProcessing(): Unit = {
    context.system.scheduler.scheduleOnce(Duration.Zero, self,
      ContinueProcessing)(context.dispatcher)
  }

  override def unhandled(message: Any): Unit = {
    val errorMessage = s"Message $message is unknown to ${getClass.getName} " +
      s"or inappropriate for state $processingDataOpt"
    println(errorMessage)
    super.unhandled(message)
  }
}

object CompressionActor {
  case class ProcessRequest(mode: ProcessingMode,
      inputArray: js.typedarray.Uint8Array, options: Options, chunkSize: Int)

  case object RequestProcessed

  protected object InternalMessages {
    case object ContinueProcessing
  }

  sealed trait ProcessingData {
    def requestSenderOpt: Option[ActorRef]
    def inputStream: ArrayInputStream
    def outputStream: Streams.ChunksArrayOutputStream
    def chunkSize: Int
  }

  class EncodingProcessingData(
      val requestSenderOpt: Option[ActorRef],
      val encoder: Encoder,
      val inputStream: ArrayInputStream,
      val outputStream: Streams.ChunksArrayOutputStream,
      val chunkSize: Int
  ) extends ProcessingData

  class DecodingProcessingData(
      val requestSenderOpt: Option[ActorRef],
      val decoder: Decoder,
      val inputStream: ArrayInputStream,
      val outputStream: Streams.ChunksArrayOutputStream,
      val chunkSize: Int
  ) extends ProcessingData

  def props(actionDispatch: CodingInProgressAction => Unit): Props =
    Props(new CompressionActor(actionDispatch, new Clock))
}
