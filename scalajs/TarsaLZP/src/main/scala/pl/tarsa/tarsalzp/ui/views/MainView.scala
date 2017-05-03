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

import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.extra.LogLifecycle
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import pl.tarsa.tarsalzp.ui.backend.MainAction._
import pl.tarsa.tarsalzp.ui.backend.MainModel.{
  ChunkCodingMeasurement,
  CodingInProgressViewData,
  IdleStateViewData
}
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.{
  DecodingMode,
  EncodingMode,
  ShowOptions
}
import pl.tarsa.tarsalzp.ui.backend._
import pl.tarsa.tarsalzp.ui.util.IdsGenerator

import scala.scalajs.js

object MainView {

  case class Props(proxy: ModelProxy[MainModel], optionsView: VdomElement,
    chartView: VdomElement)

  private def render(p: Props) = {
    val data = p.proxy()

    def foldTask[T](onIdle: IdleStateViewData => T,
      onCoding: CodingInProgressViewData => T): T = {
      data.taskViewData match {
        case codingState: CodingInProgressViewData =>
          onCoding(codingState)
        case idleState: IdleStateViewData =>
          onIdle(idleState)
      }
    }

    val busy = foldTask(_.loadingInProgress, _ => true)

    val chunkSizeLabelledSpinner = {
      LabelledSpinner(data.chunkSize, "Chunk size", event =>
        p.proxy.dispatchCB(ChangeChunkSize(event.target.valueAsNumber)), busy)
    }

    val fileChooser =
      <.input(
        ^.className := "temp_fileChooser",
        ^.`type` := "file",
        ^.disabled := busy,
        ^.onChange ==> { (e: ReactEventFromInput) =>
          println("File chooser changed!")
          p.proxy.dispatchCB(
            SelectedFile((e.target.files(0): js.UndefOr[dom.File]).toOption))
        }
      )
    val loadButton =
      <.input(
        ^.className := "temp_loadButton",
        ^.`type` := "button",
        ^.disabled := busy ||
          foldTask(_ => data.chosenFileOpt.isEmpty, _ => true),
        ^.value := "Load contents from file",
        ^.onClick --> p.proxy.dispatchCB(LoadFile))
    val processButton =
      <.input(
        ^.className := "temp_processButton",
        ^.`type` := "button",
        ^.disabled := busy || foldTask(_.inputArrayOpt.isEmpty, _ => true),
        ^.value := "Process data",
        ^.onClick --> p.proxy.dispatchCB(StartProcessing))
    val saveButton =
      <.input(
        ^.className := "temp_saveButton",
        ^.`type` := "button",
        ^.disabled := busy || foldTask(_.codingResultOpt.isEmpty, _ => true),
        ^.value := "Save results to file",
        ^.onClick --> p.proxy.dispatchCB(SaveFile))

    def makeModeSwitch(value: String, description: String,
      mode: ProcessingMode) = {
      val id = IdsGenerator.freshUnique()
      val input =
        <.input(
          ^.className := s"temp_modeSwitch_$value",
          ^.id := id,
          ^.disabled := busy,
          ^.name := "mode",
          ^.`type` := "radio",
          ^.checked := mode == data.taskViewData.mode,
          ^.value := value,
          ^.onChange --> p.proxy.dispatchCB(ChangedMode(mode))
        )
      val label =
        <.label(
          ^.`for` := id,
          description)
      (input, label)
    }

    val encodeLabelledButton = makeModeSwitch("encode", "Encode", EncodingMode)
    val decodeLabelledButton = makeModeSwitch("decode", "Decode", DecodingMode)
    val showOptionsLabelledButton =
      makeModeSwitch("showOptions", "Show options", ShowOptions)

    def codingTimeline(totalSymbolsOpt: Option[Int],
      timeline: Seq[ChunkCodingMeasurement]) = {
      <.div(s"Chunks number: ${timeline.size}")
    }

    val codingResultInfo = {
      data.taskViewData match {
        case coding: CodingInProgressViewData =>
          val elapsedMillis = js.Date.now() - coding.startTime.getTime()
          val processedSymbols =
            coding.mode match {
              case EncodingMode =>
                coding.inputStreamPosition
              case DecodingMode =>
                coding.outputStreamPosition
            }
          val speed = processedSymbols / (elapsedMillis / 1000.0)
          val totalSymbolsOpt =
            coding.mode match {
              case EncodingMode =>
                Some(coding.inputBufferLength)
              case DecodingMode =>
                None
            }
          <.div(
            codingTimeline(totalSymbolsOpt, coding.codingTimeline),
            <.div("Coding in progress:"),
            <.div(s"Coding mode: ${coding.mode}"),
            <.div("Input progress:",
              <.progress(^.value := coding.inputStreamPosition,
                ^.max := coding.inputBufferLength),
              s"${coding.inputStreamPosition} / " +
                s"${coding.inputBufferLength} bytes"
            ),
            <.div("Output progress: " +
              s"${coding.outputStreamPosition} / ??? bytes"),
            <.div(s"Coding start time: ${coding.startTime}"),
            <.div(f"Elapsed milliseconds: $elapsedMillis%.1f"),
            <.div(f"Coding speed: $speed%.1f symbols / second")
          )
        case IdleStateViewData(_, _, Some(codingResult), _) =>
          val elapsedMillis = codingResult.endTime.getTime() -
            codingResult.startTime.getTime()
          val speed = codingResult.totalSymbols / (elapsedMillis / 1000.0)
          <.div(
            codingTimeline(Some(codingResult.totalSymbols),
              codingResult.codingTimeline),
            <.div("Coding result:"),
            <.div(s"Coding mode: ${codingResult.mode}"),
            <.div(s"Coding start time: ${codingResult.startTime}"),
            <.div(s"Coding end time: ${codingResult.endTime}"),
            <.div(f"Elapsed milliseconds: $elapsedMillis%.1f"),
            <.div(s"Total symbols: ${codingResult.totalSymbols}"),
            <.div(s"Compressed size in bytes: ${codingResult.compressedSize}"),
            <.div(f"Coding speed: $speed%.1f symbols / second")
          )
        case _ =>
          <.div("No coding result")
      }
    }

    <.div(
      TagMod(
        chunkSizeLabelledSpinner.label,
        chunkSizeLabelledSpinner.spinner,
        <.br(), <.br(),
        p.optionsView
      )(
        encodeLabelledButton._1,
        encodeLabelledButton._2,
        decodeLabelledButton._1,
        decodeLabelledButton._2,
        showOptionsLabelledButton._1,
        showOptionsLabelledButton._2
      )(
        <.br(),
        fileChooser,
        loadButton,
        processButton,
        saveButton,
        <.br(), <.br(), <.br(),
        <.div(p.chartView),
        codingResultInfo
      )
    )
  }

  private val component =
    ScalaComponent.builder[Props]("MainView")
      .stateless
      .render_P(render)
      .configure(LogLifecycle.short)
      .build

  def apply(proxy: ModelProxy[MainModel],
    optionsView: VdomElement,
    chartView: VdomElement): Unmounted[Props, Unit, Unit] =
    component(Props(proxy, optionsView, chartView))
}
