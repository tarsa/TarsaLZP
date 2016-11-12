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
package pl.tarsa.tarsalzp.ui

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import pl.tarsa.tarsalzp.ui.backend.{MainModel, MainStateHolder}
import pl.tarsa.tarsalzp.ui.util.{LoggingProcessor, RafBatcher}
import pl.tarsa.tarsalzp.ui.views.{MainView, OptionsView}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object TarsaLZP extends js.JSApp {
  @JSExport
  def main(): Unit = {
    val mainCircuit = new MainStateHolder
    mainCircuit.addProcessor(new RafBatcher)
    mainCircuit.addProcessor(new LoggingProcessor)
    mainCircuit.subscribe(mainCircuit.zoom(identity[MainModel]))(model =>
      println(s"Listener got fired!"))
    val mainWrapper = mainCircuit.connect(identity[MainModel] _)
    val optionsView = {
      val optionsViewProxy = mainCircuit.connect(_.options)
      optionsViewProxy(OptionsView.apply)
    }
    val mainComponent = mainWrapper(MainView.apply(_, optionsView))
    ReactDOM.render(mainComponent, dom.document.getElementById("mainDiv"))
  }
}
