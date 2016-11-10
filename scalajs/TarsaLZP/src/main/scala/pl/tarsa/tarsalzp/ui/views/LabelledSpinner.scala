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

import japgolly.scalajs.react.{Callback, ReactEventI}
import japgolly.scalajs.react.vdom.ReactTagOf
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.html
import pl.tarsa.tarsalzp.ui.util.IdsGenerator

class LabelledSpinner(
  val label: ReactTagOf[html.Label],
  val spinner: ReactTagOf[html.Input]
)

object LabelledSpinner {
  def apply(loadValue: => Int, description: String,
    onChangeAction: ReactEventI => Callback,
    disabled: Boolean): LabelledSpinner = {
    val id = IdsGenerator.freshUnique()
    val spinner = <.input(^.id := id, ^.`type` := "number",
      ^.value := loadValue, ^.onChange ==> onChangeAction,
      ^.disabled := disabled)
    val label = <.label(^.`for` := id, description + ':')
    new LabelledSpinner(label, spinner)
  }
}
