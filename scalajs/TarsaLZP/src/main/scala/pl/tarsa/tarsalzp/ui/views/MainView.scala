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

import java.util.UUID._

import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import pl.tarsa.tarsalzp.ui.backend._
import pl.tarsa.tarsalzp.ui.util.TagModJoiner

import scala.scalajs.js

object MainView {
  case class Props(proxy: ModelProxy[MainModel])

  private def render(p: Props) = {
    val model = p.proxy()

    val fileChooser =
      <.input(
        ^.`type` := "file",
        ^.disabled := model.busy,
        ^.onChange ==> { (e: ReactEventI) =>
          println("File chooser changed!")
          p.proxy.dispatch(
            SelectedFile((e.target.files(0): js.UndefOr[dom.File]).toOption))
        }
      )
    val loadButton =
      <.input(
      ^.`type` := "button",
      ^.disabled := model.busy || model.chosenFileOpt.isEmpty,
      ^.value := "Load contents from file",
        ^.onClick --> p.proxy.dispatch(LoadFile))
    val processButton =
      <.input(
        ^.`type` := "button",
        ^.disabled := model.busy || model.buffers.inputBufferOpt.isEmpty,
      ^.value := "Process data",
        ^.onClick --> p.proxy.dispatch(ProcessFile))
    val saveButton =
      <.input(
        ^.`type` := "button",
        ^.disabled := model.busy || model.buffers.outputStreamOpt.isEmpty,
      ^.value := "Save results to file",
        ^.onClick --> p.proxy.dispatch(SaveFile))

    def makeModeSwitch(value: String, description: String,
      mode: ProcessingMode) = {
      val id = randomUUID().toString
      val input =
        <.input(
          ^.id := id,
          ^.disabled := model.busy,
          ^.name := "mode",
          ^.`type` := "radio",
          ^.value := value,
          ^.onClick --> p.proxy.dispatch(ChangedMode(mode))
        )
      val label =
        <.label(
          ^.`for` := id,
          ^.checked := mode == model.mode,
          description)
      (input, label)
    }

    val encodeLabelledButton = makeModeSwitch("encode", "Encode", Encode)
    val decodeLabelledButton = makeModeSwitch("decode", "Decode", Decode)
    val showOptionsLabelledButton =
      makeModeSwitch("showOptions", "Show options", ShowOptions)


    <.div(
      TagModJoiner(
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
        saveButton
      ): _*
    )
  }


  val component = ReactComponentB[Props]("MainView")
    .stateless
    .render_P(render)
    .build

  def apply(proxy: ModelProxy[MainModel]) = component(Props(proxy))
}
