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

import japgolly.scalajs.react.vdom.Attr.ValueType
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import org.scalajs.dom.html
import pl.tarsa.tarsalzp.ui.util.IdsGenerator

// TODO rename to LabelledNumberInput
class LabelledSpinner(
    val label: VdomTagOf[html.Label],
    val numberInput: VdomTagOf[html.Input]
)

object LabelledSpinner {
  abstract class Builder[T: Numeric](implicit t: ValueType[T, Any]) {
    def convertValue(number: Double): T

    def apply(value: T, description: String, onChangeAction: T => Callback,
        disabled: Boolean): LabelledSpinner = {
      val onChange = (event: ReactEventFromInput) =>
        onChangeAction(convertValue(event.target.value.toDouble))
      val id = IdsGenerator.freshUnique()
      new LabelledSpinner(
        <.label(^.`for` := id, s"$description:"),
        <.input(^.id := id, ^.`type` := "number", ^.value := value,
          ^.disabled := disabled, ^.onChange ==> onChange)
      )
    }
  }

  val forInts: Builder[Int] =
    new Builder[Int] {
      override def convertValue(number: Double): Int =
        number.toInt
    }
}
