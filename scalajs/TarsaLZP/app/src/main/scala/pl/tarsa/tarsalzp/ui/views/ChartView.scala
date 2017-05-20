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
import japgolly.scalajs.react.vdom.{TopNode, svg_<^ => svg}
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.d3
import pl.tarsa.tarsalzp.ui.MyStyleSheet
import pl.tarsa.tarsalzp.ui.backend.MainModel.ChunkCodingMeasurement

import scala.scalajs.js.JSConverters._
import scalacss.ScalaCssReact._

object ChartView {

  class D3Chart(svgWidth: Int, svgHeight: Int) {

    case class Rect(left: Int, top: Int, width: Int, height: Int)

    def create(element: TopNode, model: Props): Unit = {
      val svg = d3.select(element)

      svg.append("g").attr("class", ".d3-points")
      update(element, model)
    }

    def update(element: TopNode, model: Props): Unit =
      drawPoints(element, model)

    def destroy(element: TopNode): Unit =
      ()

    def drawPoints(element: TopNode, model: Props): Unit = {
      val Model(chunkSize, totalSymbolsOpt, timeline) = model.proxy()
      println(s"Timeline size: ${timeline.size}")

      val totalChunks = totalSymbolsOpt
        .map(totalSymbols => (totalSymbols + chunkSize - 1) / chunkSize)
        .getOrElse(timeline.size)

      val svg = d3.select(element)
      val g = svg.select("g")

      val point = g.selectAll("rect").data(timeline.toJSArray)

      point.enter().append("rect")

      def calculate[T](lens: Rect => T)(measurement: ChunkCodingMeasurement,
          index: Int): T = {
        val left = svgWidth * index / totalChunks
        val right = svgWidth * (index + 1) / totalChunks
        val height = (svgHeight.toLong * measurement.compressedSize /
          measurement.symbolsNumber).toInt
        val bottom = svgHeight
        lens(Rect(left, bottom - height, right - left, height))
      }

      point.attr("x", calculate(_.left.px) _)
      point.attr("y", calculate(_.top.px) _)
      point.attr("width", calculate(_.width.px) _)
      point.attr("height", calculate(_.height.px) _)

      val baseColor = d3.rgb("DarkSlateBlue")

      val rectColorFun = (_: ChunkCodingMeasurement, i: Int) =>
        baseColor.brighter(4 * Math.abs((i % 15 - 0.5) / 15.0 - 0.5)).toString

      point.style("fill", rectColorFun)

      point.exit().remove()
    }
  }

  case class Model(chunkSize: Int, totalSymbolsOpt: Option[Int],
      timeline: Seq[ChunkCodingMeasurement])

  case class Props(svgWidth: Int, svgHeight: Int, proxy: ModelProxy[Model])

  class Backend(t: BackendScope[Props, Unit]) {
    def p: Props = t.props.runNow()

    val d3chart = new D3Chart(p.svgWidth, p.svgHeight)

    def render(p: Props): VdomElement = {
      svg.<.svg(^.className := "d3", svg.^.width := p.svgWidth.px,
        svg.^.height := p.svgHeight.px, MyStyleSheet.svg)
    }

    def didMount(): Callback = {
      for {
        element <- t.getDOMNode
        model <- t.props
      } yield {
        d3chart.create(element, model)
      }
    }

    def didUpdate(): Callback = {
      for {
        element <- t.getDOMNode
        model <- t.props
      } yield {
        d3chart.update(element, model)
      }
    }

    def willUnmount(): Callback = {
      t.getDOMNode.map { element =>
        d3chart.destroy(element)
      }
    }
  }

  private val component =
    ScalaComponent
      .builder[Props]("ChartView")
      .stateless
      .renderBackend[Backend]
      .componentDidMount(_.backend.didMount())
      .componentDidUpdate(_.backend.didUpdate())
      .componentWillUnmount(_.backend.willUnmount())
      .configure(LogLifecycle.short)
      .build

  def apply(proxy: ModelProxy[Model]): VdomElement =
    component(Props(400, 400, proxy))
}
