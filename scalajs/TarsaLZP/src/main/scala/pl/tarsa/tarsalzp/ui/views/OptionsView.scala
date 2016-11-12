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
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.backend.UpdateOptions

object OptionsView {

  case class Props(proxy: ModelProxy[Options])

  private def render(p: Props) = {
    val options = p.proxy()

    def make(description: String, loadValue: Options => Int,
      saveValue: (Options, Int) => Options): LabelledSpinner = {

      def updateAction(e: ReactEventI) =
        p.proxy.dispatch(UpdateOptions(saveValue(_, e.target.valueAsNumber)))

      LabelledSpinner(loadValue(options), description, updateAction,
        disabled = false)
    }

    def lzpLowContextLength = make("LZP Low Context Length",
      _.lzpLowContextLength, (o, v) => o.copy(lzpLowContextLength = v))
    def lzpLowMaskSize = make("LZP Low Mask Size",
      _.lzpLowMaskSize, (o, v) => o.copy(lzpLowMaskSize = v))
    def lzpHighContextLength = make("LZP High Context Length",
      _.lzpHighContextLength, (o, v) => o.copy(lzpHighContextLength = v))
    def lzpHighMaskSize = make("LZP High Mask Size",
      _.lzpHighMaskSize, (o, v) => o.copy(lzpHighMaskSize = v))
    def literalCoderOrder = make("Literal Coder Order",
      _.literalCoderOrder, (o, v) => o.copy(literalCoderOrder = v))
    def literalCoderInit = make("Literal Coder Init",
      _.literalCoderInit, (o, v) => o.copy(literalCoderInit = v))
    def literalCoderStep = make("Literal Coder Step",
      _.literalCoderStep, (o, v) => o.copy(literalCoderStep = v))
    def literalCoderLimit = make("Literal Coder Limit",
      _.literalCoderLimit, (o, v) => o.copy(literalCoderLimit = v))

    def table = {
      val labelledSpinners =
        Seq(lzpLowContextLength, lzpLowMaskSize,
          lzpHighContextLength, lzpHighMaskSize, literalCoderOrder,
          literalCoderInit, literalCoderStep, literalCoderLimit
        ).map {
          labelledSpinner =>
            <.tr(
              <.td(labelledSpinner.label),
              <.td(labelledSpinner.spinner)
            )
        }
      val statusRow = <.tr(
        <.td("Options status:"),
        <.td(
          if (options.isValid) {
            "Valid"
          } else {
            "Invalid"
          }
        )
      )
      <.table(
        <.tbody(
          labelledSpinners :+ statusRow: _*
        )
      )
    }

    table
  }

  private val component = ReactComponentB[Props]("OptionsView")
    .stateless
    .render_P(render)
    .configure(LogLifecycle.short)
    .build

  def apply(proxy: ModelProxy[Options]) =
    component(Props(proxy))
}
