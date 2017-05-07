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

import _infrastructure.data.Models
import _infrastructure.domtest.DomNodeInfo
import _infrastructure.domtest.DomNodeInfo.{A, F}
import _infrastructure.domtest.Extractor.{H, N}
import _infrastructure.specs.FrontendSpecBase
import diode.react.{ModelProxy, ReactConnector}
import diode.{ActionResult, ActionType, Circuit}
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.test.{ReactTestUtils, Simulate}
import org.scalajs.dom
import pl.tarsa.tarsalzp.ui.backend.MainAction.{
  LoadFile,
  SelectedFile,
  StartProcessing
}
import pl.tarsa.tarsalzp.ui.backend.MainModel.CodingInProgressViewData
import pl.tarsa.tarsalzp.ui.backend.{MainAction, MainModel}

import scala.collection.mutable
import scala.scalajs.js

class MainViewSpec extends FrontendSpecBase {
  typeBehavior[MainView.type]

  it must s"show initial idle state" in {
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
        case H.span(H.label(labelText @ N.text()), radioButton @ H.input()) =>
          labelText.mustHaveProps(F.wholeText("Encode"))
          radioButton.mustHaveProps(F.checked(true))
      }
      inside(decodeControl) {
        case H.span(H.label(labelText @ N.text()), radioButton @ H.input()) =>
          labelText.mustHaveProps(F.wholeText("Decode"))
          radioButton.mustHaveProps(F.checked(false))
      }
      inside(showOptionsControl) {
        case H.span(H.label(labelText @ N.text()), radioButton @ H.input()) =>
          labelText.mustHaveProps(F.wholeText("Show options"))
          radioButton.mustHaveProps(F.checked(false))
      }
      fileChooser.mustHaveProps(A.tpe("file"))
      loadContentsButton.mustHaveProps(A.tpe("button"),
        A.value("Load contents from file"), F.disabled(true))
      processDataButton.mustHaveProps(A.tpe("button"), A.value("Process data"),
        F.disabled(true))
      saveResultsButton.mustHaveProps(A.tpe("button"),
        A.value("Save results to file"), F.disabled(true))
      inside(codingResult) {
        case H.div(text @ N.text()) =>
          text.mustHaveProps(F.wholeText("No coding result"))
      }
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

  it must "show initial encoding state" in {
    val model = Models.afterStartedEncoding
    val viewData = model.taskViewData.asInstanceOf[CodingInProgressViewData]
    withModel(model) { fixture =>
      import fixture._
      import mainViewInfo._
      val progressRow = inside(codingResult) {
        case H.div(H.div(row1 @ N.text()), H.div(row2 @ N.text()),
            H.div(row3 @ N.text()), progressRow @ H.div(_ *),
            H.div(row5 @ N.text()), H.div(row6 @ N.text()),
            H.div(row7 @ N.text()), H.div(row8 @ N.text())) =>
          row1.mustHaveProps(F.wholeText("Chunks number: 0"))
          row2.mustHaveProps(F.wholeText("Coding in progress:"))
          row3.mustHaveProps(F.wholeText("Coding mode: EncodingMode"))
          row5.mustHaveProps(F.wholeText("Output progress: 0 / ??? bytes"))
          val startTime = viewData.startTime
          row6.mustHaveProps(F.wholeText(s"Coding start time: $startTime"))
          row7.propFor(F.wholeText).get must fullyMatch regex
            """Elapsed milliseconds: \d*\.\d"""
          row8.mustHaveProps(F.wholeText("Coding speed: 0.0 symbols / second"))
          progressRow
      }
      inside(progressRow) {
        case H.div(N.comment(), leftText @ N.text(), N.comment(),
            progressBar @ H.progress(), N.comment(), rightText @ N.text(),
            N.comment()) =>
          leftText.mustHaveProps(F.wholeText("Input progress:"))
          progressBar.mustHaveProps(A.value(0), A.max(3))
          rightText.mustHaveProps(F.wholeText("0 / 3 bytes"))
      }
    }
  }

  def extractMainViewInfo[Repr <: DomNodeInfo[Repr]](
      domNodeInfo: Repr): MainViewInfo[Repr] = {
    inside(domNodeInfo) {
      case H.div(
          chunkSizeControl @ H.div(_ *),
          H.br(),
          H.noscript(),
          H.div(encodeControl @ H.span(_ *), decodeControl @ H.span(_ *),
            showOptionsControl @ H.span(_ *)),
          H.div(fileChooser @ H.input(), loadContentsButton @ H.input(),
            processDataButton @ H.input(), saveResultsButton @ H.input()),
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
