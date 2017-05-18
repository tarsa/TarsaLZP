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

import _infrastructure.data.Models
import _infrastructure.specs.FixtureAkkaSpecBase
import akka.testkit.TestProbe
import diode.ActionResult.{ModelUpdate, ModelUpdateEffect}
import diode._
import org.scalatest.FutureOutcome
import pl.tarsa.tarsalzp.compression.CompressionActor
import pl.tarsa.tarsalzp.compression.options.Options

import scala.concurrent.duration._

class MainActionHandlerSpec extends FixtureAkkaSpecBase {
  typeBehavior[MainActionHandler[MainModel]]

  it must "update options" in { fixture =>
    fixture.handleFunction(
      Models.initial,
      MainAction.UpdateOptions(
          Options.Updater.NewLiteralCoderLimit(
              Models.Parameters.newLiteralCoderLimit))
    ) mustBe Some(ModelUpdate(Models.optionsUpdated))
  }

  it must "change chunks size" in { fixture =>
    fixture.handleFunction(
      Models.initial,
      MainAction.ChangeChunkSize(Models.Parameters.newChunkSize)
    ) mustBe Some(ModelUpdate(Models.chunksSizeChanged))
  }

  it must "change mode" in { fixture =>
    fixture.handleFunction(
      Models.initial,
      MainAction.ChangedMode(Models.Parameters.newMode)
    ) mustBe Some(ModelUpdate(Models.modeChanged))
  }

  it must "select file" in { fixture =>
    fixture.handleFunction(
      Models.initial,
      MainAction.SelectedFile(Models.Parameters.newChosenFile)
    ) mustBe Some(ModelUpdate(Models.fileSelected))
  }

  it must "load file" in { fixture =>
    val effect = inside(
      fixture.handleFunction(
        Models.fileSelected,
        MainAction.LoadFile
      )
    ) {
      case Some(result @ ModelUpdateEffect(Models.fileLoadingStarted, _)) =>
        result.effect
    }
    effect.toFuture.map { result =>
      inside(result) {
        case message @ MainAction.LoadingFinished(Some(_)) =>
          fixture.handleFunction(
            Models.fileLoadingStarted,
            message
          ) mustBe Some(ModelUpdate(Models.fileLoadingFinished))
      }
    }
  }

  it must "start processing" in { fixture =>
    val viewData = inside(
      fixture.handleFunction(
        Models.fileLoadingFinished,
        MainAction.StartProcessing
      )
    ) {
      case Some(ModelUpdate(updatedModel)) =>
        inside(updatedModel) {
          case MainModel(_, _, _,
              viewData: MainModel.CodingInProgressViewData) =>
            val withCopiedTimestamp = updatedModel.copy(
                taskViewData = viewData.copy(
                    startTime = Models.Parameters.encodingStartTime))
            withCopiedTimestamp mustBe Models.encodingStarted
            viewData
        }
    }
    fixture.compressionActorProbe.receiveOne(1.second) mustBe
      CompressionActor.ProcessRequest(ProcessingMode.EncodingMode,
        viewData.wrappedInput.raw, Options.default,
        Models.Parameters.initialChunkSize)
  }

  it must "handle first measurement" in { fixture =>
    fixture.handleFunction(
      Models.encodingStarted,
      MainAction.ChunkProcessed(Models.Parameters.firstMeasurement)
    ) mustBe Some(ModelUpdate(Models.firstMeasurementReceived))
  }

  it must "handle second measurements" in { fixture =>
    fixture.handleFunction(
      Models.firstMeasurementReceived,
      MainAction.ChunkProcessed(Models.Parameters.secondMeasurement)
    ) mustBe Some(ModelUpdate(Models.secondMeasurementReceived))
  }

  it must "finish processing" in { fixture =>
    fixture.handleFunction(
      Models.secondMeasurementReceived,
      MainAction.ProcessingFinished(Models.Parameters.encodingEndTime,
        Models.Parameters.result)
    ) mustBe Some(ModelUpdate(Models.afterProcessingFinished))
  }

  class FixtureParam(
      val handleFunction: (MainModel, Any) => Option[ActionResult[MainModel]],
      val compressionActorProbe: TestProbe
  )

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val compressionActorProbe = TestProbe()
    val handleFunction =
      new Circuit[MainModel] {
        override protected def initialModel: MainModel = null

        override def actionHandler: HandlerFunction =
          new MainActionHandler(
              zoomRW(identity)((_, m) => m), compressionActorProbe.ref)
      }.actionHandler
    val fixture = new FixtureParam(handleFunction, compressionActorProbe)
    withFixture(test.toNoArgAsyncTest(fixture))
  }
}
