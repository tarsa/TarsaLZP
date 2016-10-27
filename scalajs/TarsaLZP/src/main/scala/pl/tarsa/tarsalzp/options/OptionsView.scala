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
package pl.tarsa.tarsalzp.options

import java.util.UUID.randomUUID

import org.scalajs.dom
import org.scalajs.dom.html
import pl.tarsa.imports.ScalaTagsJsDom._

object OptionsView {
  var currentOptions = Options.default

  class LabelledSpinner(
    val label: html.Label,
    val spinner: html.Input
  )

  def make(description: String): LabelledSpinner = {
    val spinner = <.input(^.id := randomUUID().toString,
      ^.`type` := "number").render
    val label = <.label(^.`for` := spinner.id, description + ':').render

    spinner.addEventListener("input", refreshAction _)

    new LabelledSpinner(label, spinner)
  }

  val lzpLowContextLength = make("LZP Low Context Length")
  val lzpLowMaskSize = make("LZP Low Mask Size")
  val lzpHighContextLength = make("LZP High Context Length")
  val lzpHighMaskSize = make("LZP High Mask Size")
  val literalCoderOrder = make("Literal Coder Order")
  val literalCoderInit = make("Literal Coder Init")
  val literalCoderStep = make("Literal Coder Step")
  val literalCoderLimit = make("Literal Coder Limit")

  val status = <.span().render

  def refreshAction(event: dom.Event) = {
    refreshModel()
    refreshStatus()
    println(currentOptions)
  }

  def refreshModel(): Unit = {
    def getValue(labelledSpinner: LabelledSpinner): Int =
      labelledSpinner.spinner.value.toInt

    currentOptions = Options(
      getValue(lzpLowContextLength),
      getValue(lzpLowMaskSize),
      getValue(lzpHighContextLength),
      getValue(lzpHighMaskSize),
      getValue(literalCoderOrder),
      getValue(literalCoderInit),
      getValue(literalCoderStep),
      getValue(literalCoderLimit)
    )
  }

  def refreshStatus(): Unit = {
    status.innerHTML = {
      if (currentOptions.isValid) {
        "Valid"
      } else {
        "Invalid"
      }
    }
  }

  def refreshView(): Unit = {
    def refreshSingle(labelledSpinner: LabelledSpinner,
      extractor: Options => Int): Unit = {
      labelledSpinner.spinner.valueAsNumber = extractor(currentOptions)
    }
    refreshSingle(lzpLowContextLength, _.lzpLowContextLength)
    refreshSingle(lzpLowMaskSize, _.lzpLowMaskSize)
    refreshSingle(lzpHighContextLength, _.lzpHighContextLength)
    refreshSingle(lzpHighMaskSize, _.lzpHighMaskSize)
    refreshSingle(literalCoderOrder, _.literalCoderOrder)
    refreshSingle(literalCoderInit, _.literalCoderInit)
    refreshSingle(literalCoderStep, _.literalCoderStep)
    refreshSingle(literalCoderLimit, _.literalCoderLimit)
    refreshStatus()
  }

  refreshView()

  val table = {
    val labelledSpinners =
      Seq(lzpLowContextLength, lzpLowMaskSize,
        lzpHighContextLength, lzpHighMaskSize, literalCoderOrder,
        literalCoderInit, literalCoderStep, literalCoderLimit
      ).map {
        labelledSpinner =>
          <.tr(
            <.td(labelledSpinner.label),
            <.td(labelledSpinner.spinner)
          ).render
      }
    val statusRow = <.tr(
      <.td("Options status:"),
      <.td(status)
    ).render
    <.table(
      (labelledSpinners :+ statusRow)
        .map(implicitly[html.Element => Modifier]): _*
    ).render
  }
}
