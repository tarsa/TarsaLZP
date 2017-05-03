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
package pl.tarsa.tarsalzp.ui.views

import _infrastructure.domtest.DomNodeInfo
import _infrastructure.domtest.DomNodeInfo.{A, F}
import _infrastructure.domtest.Extractor.{H, N}
import _infrastructure.specs.FrontendSpecBase
import diode._
import diode.react.{ModelProxy, ReactConnector}
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.test._
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.backend.MainAction.{
  LoadFile,
  SelectedFile,
  StartProcessing
}
import pl.tarsa.tarsalzp.ui.backend.MainModel.IdleStateViewData
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.EncodingMode
import pl.tarsa.tarsalzp.ui.backend.{MainAction, MainModel}
import pl.tarsa.tarsalzp.ui.views.MainViewSpec.Models

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

class MainViewSpec extends FrontendSpecBase {
  typeBehavior[MainView.type]

  it must s"show initial state properly" in {
    withModel(Models.initialModel) { fixture =>
      import fixture._
      import mainViewInfo._
      inside(chunkSizeControl) {
        case H.div(H.label(chunkSizeLabelText @ N.text()),
            chunkSizeSpinner @ H.input()) =>
          chunkSizeLabelText.mustHaveProps(F.wholeText("Chunk size:"))
          chunkSizeSpinner.mustHaveProps(A.value(345))
      }
      inside(encodeControl) {
        case H.span(H.label(encodeLabelText @ N.text()), H.input()) =>
          encodeLabelText.mustHaveProps(F.wholeText("Encode"))
      }
      inside(decodeControl) {
        case H.span(H.label(decodeLabelText @ N.text()), H.input()) =>
          decodeLabelText.mustHaveProps(F.wholeText("Decode"))
      }
      inside(showOptionsControl) {
        case H.span(H.label(showOptionsLabelText @ N.text()), H.input()) =>
          showOptionsLabelText.mustHaveProps(F.wholeText("Show options"))
      }
      fileChooser.mustHaveProps(A.`type`("file"))
      loadContentsButton.mustHaveProps(A.`type`("button"),
        A.value("Load contents from file"), F.disabled(true))
      processDataButton.mustHaveProps(A.`type`("button"),
        A.value("Process data"), F.disabled(true))
      saveResultsButton.mustHaveProps(A.`type`("button"),
        A.value("Save results to file"), F.disabled(true))
    }
  }

  it must s"fire event after selecting file" in {
    withModel(Models.initialModel) { fixture =>
      import fixture._
      val file = new dom.Blob().asInstanceOf[dom.File]
      val target = js.Dynamic.literal("files" -> js.Array(file))
      val eventData = js.Dynamic.literal("target" -> target)
      val fileChooserDom =
        findChildByClassName(mainViewNode, "temp_fileChooser")
      Simulate.change(fileChooserDom, eventData)
      testCircuit.actionsQueue mustBe Seq(SelectedFile(Some(file)))
    }
  }

  it must "allow loading file after it was selected" in {
    withModel(Models.afterFileSelection) { fixture =>
      import fixture._
      import mainViewInfo._
      loadContentsButton.mustHaveProps(F.disabled(false))
      val loadButtonDom = findChildByClassName(mainViewNode, "temp_loadButton")
      Simulate.click(loadButtonDom)
      testCircuit.actionsQueue mustBe Seq(LoadFile)
    }
  }

  it must "disable buttons during file loading" in {
    withModel(Models.duringFileLoading) { fixture =>
      import fixture._
      import mainViewInfo._
      loadContentsButton.mustHaveProps(F.disabled(true))
      processDataButton.mustHaveProps(F.disabled(true))
      saveResultsButton.mustHaveProps(F.disabled(true))
    }
  }

  it must "allow processing after file has been loaded" in {
    withModel(Models.withLoadedFile) { fixture =>
      import fixture._
      import mainViewInfo._
      processDataButton.mustHaveProps(F.disabled(false))
      val processButtonDom =
        findChildByClassName(mainViewNode, "temp_processButton")
      Simulate.click(processButtonDom)
      testCircuit.actionsQueue mustBe Seq(StartProcessing)
    }
  }

  def extractMainViewInfo[Repr <: DomNodeInfo[Repr]](
      domNodeInfo: Repr): MainViewInfo[Repr] = {
    inside(domNodeInfo) {
      case H.div(
          chunkSizeControl @ H.div(_ *),
          H.br(),
          H.noscript(),
          H.div(
            encodeControl @ H.span(_ *),
            decodeControl @ H.span(_ *),
            showOptionsControl @ H.span(_ *)
          ),
          H.div(
            fileChooser @ H.input(),
            loadContentsButton @ H.input(),
            processDataButton @ H.input(),
            saveResultsButton @ H.input()
          ),
          H.br(),
          H.div(H.noscript()),
          codingResult @ H.div(_ *)
          ) =>
        MainViewInfo(chunkSizeControl, encodeControl, decodeControl,
          showOptionsControl, fileChooser, loadContentsButton,
          processDataButton, saveResultsButton, codingResult)
    }
  }

  case class MainViewInfo[Repr <: DomNodeInfo[Repr]](chunkSizeControl: Repr,
      encodeControl: Repr, decodeControl: Repr, showOptionsControl: Repr,
      fileChooser: Repr, loadContentsButton: Repr, processDataButton: Repr,
      saveResultsButton: Repr, codingResult: Repr)

  class Fixture[Repr <: DomNodeInfo[Repr]](
      val testCircuit: TestCircuit[MainModel],
      val mainViewNode: dom.Element,
      val mainViewInfo: MainViewInfo[Repr]
  )

  def withModelAndRepr[Repr <: DomNodeInfo[Repr]](model: MainModel,
      domNodeInfoProvider: dom.Node => Repr)(
      body: Fixture[Repr] => Unit): Unit = {
    val (testCircuit, unmountedMain) = setupDiodeWithComponent(model)
    ReactTestUtils.withRenderedIntoBody(unmountedMain) { mounted =>
      val mainViewNode = mounted.getDOMNode
      val domNodeInfo = domNodeInfoProvider(mainViewNode)
      val mainViewInfo = extractMainViewInfo(domNodeInfo)
      val fixture = new Fixture(testCircuit, mainViewNode, mainViewInfo)
      body(fixture)
    }
  }

  private val withModel =
    withModelAndRepr(_: MainModel, DomNodeInfo.lazyMutableWrapper) _

  class TestCircuit[Model <: AnyRef](val initialModel: Model)
      extends Circuit[Model]
      with ReactConnector[Model] {
    val actionsQueue: mutable.Queue[MainAction] =
      new mutable.Queue[MainAction]()

    override protected def actionHandler: HandlerFunction = {
      case (_, action: MainAction) =>
        actionsQueue += action
        Some(ActionResult.NoChange)
    }
  }

  implicit object aType extends ActionType[Any]

  def setupDiodeWithComponent(model: MainModel)
    : (TestCircuit[MainModel], Unmounted[MainView.Props, Unit, Unit]) = {
    val testCircuit = new TestCircuit(model)
    val unmountedMain = MainView(
      ModelProxy(testCircuit.zoom(x => x), testCircuit.dispatch[Any],
        testCircuit),
      dummyTag()._2,
      dummyTag()._2
    )
    (testCircuit, unmountedMain)
  }
}

object MainViewSpec {
  object Models {
    private val initialTaskViewData =
      IdleStateViewData(EncodingMode, None, None, loadingInProgress = false)

    val initialModel =
      MainModel(Options.default, None, 345, initialTaskViewData)

    val afterFileSelection: MainModel = initialModel.copy(
      chosenFileOpt = Some(new dom.Blob().asInstanceOf[dom.File])
    )

    val duringFileLoading: MainModel = afterFileSelection.copy(
      taskViewData = initialTaskViewData.copy(loadingInProgress = true)
    )

    val withLoadedFile: MainModel = initialModel.copy(
        taskViewData = initialTaskViewData.copy(
            inputArrayOpt = Some(new Uint8Array(3))))
  }
}
