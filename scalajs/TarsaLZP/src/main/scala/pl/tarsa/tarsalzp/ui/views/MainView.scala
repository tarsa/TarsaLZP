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
package pl.tarsa.tarsalzp.ui.views

import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.LogLifecycle
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import pl.tarsa.tarsalzp.ui.backend._
import pl.tarsa.tarsalzp.ui.util.{IdsGenerator, TagModJoiner}

import scala.scalajs.js

object MainView {

  case class Props(proxy: ModelProxy[MainModel])

  private def render(p: Props) = {
    val model = p.proxy()
    def foldTask[T](onIdle: IdleState => T,
      onCoding: CodingInProgress[_] => T): T = {
      model.currentTask match {
        case codingState: CodingInProgress[_] =>
          onCoding(codingState)
        case idleState: IdleState =>
          onIdle(idleState)
      }
    }

    val busy = foldTask(_.loadingInProgress, _ => true)

    val chunkSizeLabelledSpinner = {
      LabelledSpinner(model.chunkSize, "Chunk size", event =>
        p.proxy.dispatch(ChangeChunkSize(event.target.valueAsNumber)), busy)
    }

    val fileChooser =
      <.input(
        ^.`type` := "file",
        ^.disabled := busy,
        ^.onChange ==> { (e: ReactEventI) =>
          println("File chooser changed!")
          p.proxy.dispatch(
            SelectedFile((e.target.files(0): js.UndefOr[dom.File]).toOption))
        }
      )
    val loadButton =
      <.input(
        ^.`type` := "button",
        ^.disabled := busy ||
          foldTask(_ => model.chosenFileOpt.isEmpty, _ => true),
        ^.value := "Load contents from file",
        ^.onClick --> p.proxy.dispatch(LoadFile))
    val processButton =
      <.input(
        ^.`type` := "button",
        ^.disabled := busy || foldTask(_.inputBufferOpt.isEmpty, _ => true),
        ^.value := "Process data",
        ^.onClick --> p.proxy.dispatch(StartProcessing))
    val saveButton =
      <.input(
        ^.`type` := "button",
        ^.disabled := busy || foldTask(_.codingResultOpt.isEmpty, _ => true),
        ^.value := "Save results to file",
        ^.onClick --> p.proxy.dispatch(SaveFile))

    def makeModeSwitch(value: String, description: String,
      mode: ProcessingMode) = {
      val id = IdsGenerator.freshUnique()
      val input =
        <.input(
          ^.id := id,
          ^.disabled := busy,
          ^.name := "mode",
          ^.`type` := "radio",
          ^.value := value,
          ^.onClick --> p.proxy.dispatch(ChangedMode(mode))
        )
      val label =
        <.label(
          ^.`for` := id,
          ^.checked := mode == model.currentTask.mode,
          description)
      (input, label)
    }

    val encodeLabelledButton = makeModeSwitch("encode", "Encode", EncodingMode)
    val decodeLabelledButton = makeModeSwitch("decode", "Decode", DecodingMode)
    val showOptionsLabelledButton =
      makeModeSwitch("showOptions", "Show options", ShowOptions)

    val codingResultInfo = {
      model.currentTask match {
        case coding: CodingInProgress[_] =>
          val elapsedMillis = js.Date.now() - coding.startTime.getTime()
          val speed = coding.processedSymbols / (elapsedMillis / 1000.0)
          <.div(
            <.div("Coding in progress:"),
            <.div(s"Coding mode: ${coding.mode}"),
            <.div(s"Coding start time: ${coding.startTime}"),
            <.div(f"Elapsed milliseconds: $elapsedMillis%.1f"),
            <.div(s"Processed symbols: ${coding.processedSymbols}"),
            <.div(f"Coding speed: $speed%.1f symbols / second")
          )
        case IdleState(_, _, Some(codingResult), _) =>
          val elapsedMillis = codingResult.endTime.getTime() -
            codingResult.startTime.getTime()
          val speed = codingResult.totalSymbols / (elapsedMillis / 1000.0)
          <.div(
            <.div("Coding result:"),
            <.div(s"Coding mode: ${codingResult.mode}"),
            <.div(s"Coding start time: ${codingResult.startTime}"),
            <.div(s"Coding end time: ${codingResult.endTime}"),
            <.div(f"Elapsed milliseconds: $elapsedMillis%.1f"),
            <.div(s"Total symbols: ${codingResult.totalSymbols}"),
            <.div(f"Coding speed: $speed%.1f symbols / second")
          )
        case _ =>
          <.div("No coding result")
      }
    }

    <.div(
      TagModJoiner(
        chunkSizeLabelledSpinner.label,
        chunkSizeLabelledSpinner.spinner,
        <.br(), <.br(),
        OptionsView(p.proxy.zoom(_.options))
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
        codingResultInfo
      ): _*
    )
  }


  val component = ReactComponentB[Props]("MainView")
    .stateless
    .render_P(render)
    .configure(LogLifecycle.short)
    .build

  def apply(proxy: ModelProxy[MainModel]) = component(Props(proxy))
}
