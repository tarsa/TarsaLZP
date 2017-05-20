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

import _infrastructure.data.CodingExample
import _infrastructure.specs.AkkaSpecBase
import akka.testkit.{TestActorRef, TestProbe}
import org.scalajs.dom
import org.scalatest.Assertion
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.data.{BlobSource, WrappedDate}
import pl.tarsa.tarsalzp.system.Clock
import pl.tarsa.tarsalzp.ui.backend.MainAction.{
  ChunkProcessed,
  ProcessingFinished
}
import pl.tarsa.tarsalzp.ui.backend.MainModel.ChunkCodingMeasurement
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode

import scala.collection.mutable
import scala.scalajs.js

class CompressionActorSpec extends AkkaSpecBase {
  typeBehavior[CompressionActor]

  it must "encode" in codingTest(CodingExample.original, CodingExample.encoded,
    ProcessingMode.EncodingMode, 1659)

  it must "decode" in codingTest(CodingExample.encoded, CodingExample.original,
    ProcessingMode.DecodingMode, 1664)

  def codingTest(inputJsArray: js.Array[Byte], resultJsArray: js.Array[Byte],
      processingMode: ProcessingMode.WithCodingMode,
      firstChunkCompressedSizeMeasurement: Int): Assertion = {
    val inputArray = new js.typedarray.Uint8Array(inputJsArray)
    val recordedMessages = mutable.Queue[Any]()
    val clockSamples = mutable.Queue(123, 234, 345, 456, 567, 678, 789)
    val Seq(date1, date2, date3, date4, date5, date6, date7) =
      clockSamples.map(millis => new WrappedDate(new js.Date(millis)))
    val clock = new Clock {
      override protected def millisecondsNow: Double =
        clockSamples.dequeue()
    }
    val compressionActor =
      TestActorRef(new CompressionActor(recordedMessages += _, clock))
    val options = Options.default
    val chunkSize = 3456
    val requesterProbe = TestProbe()
    val requestMessage =
      CompressionActor.ProcessRequest(processingMode,
                                      inputArray,
                                      options,
                                      chunkSize)
    compressionActor.tell(requestMessage, requesterProbe.ref)
    requesterProbe.expectMsg(CompressionActor.RequestProcessed)
    val result = new BlobSource {
      override def toIterator: Iterator[Byte] =
        resultJsArray.toIterator

      override def toBlob: dom.Blob =
        new dom.Blob()
    }
    inside(recordedMessages) {
      case Seq(chunkResult1, chunkResult2, chunkResult3, finishResult) =>
        chunkResult1 mustBe ChunkProcessed(
          ChunkCodingMeasurement(date1,
                                 date2,
                                 3456,
                                 firstChunkCompressedSizeMeasurement))
        chunkResult2 mustBe ChunkProcessed(
          ChunkCodingMeasurement(date3, date4, 3456, 972))
        chunkResult3 mustBe ChunkProcessed(
          ChunkCodingMeasurement(date5, date7, 2472, 664))
        finishResult mustBe ProcessingFinished(date6, result)
    }
  }
}
