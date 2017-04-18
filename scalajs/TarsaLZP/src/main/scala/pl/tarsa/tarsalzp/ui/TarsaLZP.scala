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
package pl.tarsa.tarsalzp.ui

import akka.actor.ActorSystem
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.backend.MainModel.{
  CodingInProgressViewData,
  IdleStateViewData
}
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.{DecodingMode, EncodingMode}
import pl.tarsa.tarsalzp.ui.backend._
import pl.tarsa.tarsalzp.ui.util.DiodeTypes.DiodeWrapperU
import pl.tarsa.tarsalzp.ui.util.{LoggingProcessor, RafBatcher}
import pl.tarsa.tarsalzp.ui.views.ChartView.Model
import pl.tarsa.tarsalzp.ui.views.{ChartView, MainView, OptionsView}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object TarsaLZP {
  def main(system: ActorSystem): Unit = {
    MyStyleSheet.addToDocument()

    val diodeCircuit = setupCircuit(system)

    val optionsView = makeOptionsView(diodeCircuit)
    val chartView = makeChartView(diodeCircuit)
    val mainView = makeMainView(diodeCircuit, optionsView, chartView)

    ReactDOM.render(mainView, dom.document.getElementById("mainDiv"))
  }

  private def setupCircuit(system: ActorSystem): MainStateHolder = {
    val diodeCircuit = new MainStateHolder(system)
    diodeCircuit.addProcessor(new RafBatcher)
    diodeCircuit.addProcessor(new LoggingProcessor)
    diodeCircuit.subscribe(diodeCircuit.zoom(identity[MainModel]))(
        _ => println(s"Listener got fired!"))
    diodeCircuit
  }

  private def makeOptionsView(
      diodeCircuit: MainStateHolder): DiodeWrapperU[Options] = {
    val optionsViewProxyConstructor = diodeCircuit.connect(_.options)
    optionsViewProxyConstructor(OptionsView.apply)
  }

  private def makeChartView(
      diodeCircuit: MainStateHolder): DiodeWrapperU[ChartView.Model] = {
    val modelReader = diodeCircuit.zoom { viewData =>
      val chunkSize = viewData.chunkSize
      val (totalSymbolsOpt, timeline) = viewData.taskViewData match {
        case codingInProgress: CodingInProgressViewData =>
          val totalSymbolsOpt = codingInProgress.mode match {
            case EncodingMode =>
              Some(codingInProgress.inputBufferLength)
            case DecodingMode =>
              None
          }
          (totalSymbolsOpt, codingInProgress.codingTimeline)
        case IdleStateViewData(_, _, Some(codingResult), _) =>
          (Some(codingResult.totalSymbols), codingResult.codingTimeline)
        case IdleStateViewData(_, _, None, _) =>
          (None, Nil)
      }
      Model(chunkSize, totalSymbolsOpt, timeline)
    }
    val chartViewProxyConstructor = diodeCircuit.connect(modelReader)
    chartViewProxyConstructor(ChartView.apply)
  }

  private def makeMainView(
      diodeCircuit: MainStateHolder,
      optionsView: DiodeWrapperU[Options],
      chartView: DiodeWrapperU[ChartView.Model]): DiodeWrapperU[MainModel] = {
    val mainViewProxyConstructor = diodeCircuit.connect(identity[MainModel] _)
    mainViewProxyConstructor(MainView.apply(_, optionsView, chartView))
  }
}
