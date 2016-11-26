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
import pl.tarsa.tarsalzp.ui.backend._
import pl.tarsa.tarsalzp.ui.util.{LoggingProcessor, RafBatcher}
import pl.tarsa.tarsalzp.ui.views.ChartView.Model
import pl.tarsa.tarsalzp.ui.views.{ChartView, MainView, OptionsView}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

@JSExport
object TarsaLZP extends js.JSApp {
  @JSExport
  def main(): Unit = {
    MyStyleSheet.addToDocument()

    val mainCircuit = new MainStateHolder
    mainCircuit.addProcessor(new RafBatcher)
    mainCircuit.addProcessor(new LoggingProcessor)
    mainCircuit.subscribe(mainCircuit.zoom(identity[MainModel]))(model =>
      println(s"Listener got fired!"))

    val mainViewProxy = mainCircuit.connect(_.viewData)
    val optionsView = {
      val optionsViewProxy = mainCircuit.connect(_.viewData.options)
      optionsViewProxy(OptionsView.apply)
    }
    val chartView = {
      val modelReader = mainCircuit.zoom(_.viewData).zoom { viewData =>
        val chunkSize = viewData.chunkSize
        val (totalSymbolsOpt, timeline) = viewData.taskViewData match {
          case idleState: IdleStateViewData =>
            idleState.codingResultOpt.map { codingResult =>
              (Option(codingResult.totalSymbols), codingResult.codingTimeline)
            }.getOrElse((None, Nil))
          case codingInProgress: CodingInProgressViewData =>
            val totalSymbolsOpt = codingInProgress.mode match {
              case EncodingMode =>
                Some(codingInProgress.inputBufferLength)
              case DecodingMode =>
                None
            }
            (totalSymbolsOpt, codingInProgress.codingTimeline)
        }
        Model(chunkSize, totalSymbolsOpt, timeline)
      }
      val chartViewProxy = mainCircuit.connect(modelReader)
      chartViewProxy(ChartView.apply)
    }
    val mainComponent = mainViewProxy(MainView.apply(_, optionsView, chartView))
    ReactDOM.render(mainComponent, dom.document.getElementById("mainDiv"))
  }
}
