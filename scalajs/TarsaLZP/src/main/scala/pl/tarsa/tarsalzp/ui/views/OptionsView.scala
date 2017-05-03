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
import japgolly.scalajs.react.extra.LogLifecycle
import japgolly.scalajs.react.vdom.html_<^._
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.backend.MainAction.UpdateOptions

object OptionsView {

  case class Props(proxy: ModelProxy[Options])

  private def render(p: Props) = {
    val options = p.proxy()

    def optionInput(description: String, loadValue: Options => Int,
        saveValue: Int => Options.Updater) = {
      def updateAction(value: Int) =
        p.proxy.dispatchCB(UpdateOptions(saveValue(value)))
      val labelledNumberInput = LabelledNumberInput.forInts(loadValue(options),
        description, updateAction, disabled = false)
      <.tr(
        <.td(labelledNumberInput.label),
        <.td(labelledNumberInput.numberInput)
      )
    }

    val parameters = {
      import Options.Updater._
      TagMod(
        optionInput("LZP Low Context Length", _.lzpLowContextLength,
          NewLzpLowContextLength),
        optionInput("LZP Low Mask Size", _.lzpLowMaskSize, NewLzpLowMaskSize),
        optionInput("LZP High Context Length", _.lzpHighContextLength, NewLzpHighContextLength),
        optionInput("LZP High Mask Size", _.lzpHighMaskSize, NewLzpHighMaskSize),
        optionInput("Literal Coder Order", _.literalCoderOrder, NewLiteralCoderOrder),
        optionInput("Literal Coder Init", _.literalCoderInit, NewLiteralCoderInit),
        optionInput("Literal Coder Step", _.literalCoderStep, NewLiteralCoderStep),
        optionInput("Literal Coder Limit", _.literalCoderLimit, NewLiteralCoderLimit)
      )
    }

    val statusRow =
      <.tr(
        <.td("Options status:"),
        <.td(if (options.isValid) "Valid" else "Invalid")
      )

    <.table(
      <.tbody(parameters, statusRow)
    )
  }

  private val component = ScalaComponent
    .builder[Props]("OptionsView")
    .stateless
    .render_P(render)
    .configure(LogLifecycle.short)
    .build

  def apply(proxy: ModelProxy[Options]): VdomElement =
    component(Props(proxy))
}
