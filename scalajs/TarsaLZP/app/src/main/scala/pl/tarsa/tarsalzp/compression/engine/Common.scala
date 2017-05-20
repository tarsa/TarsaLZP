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
package pl.tarsa.tarsalzp.compression.engine

import pl.tarsa.tarsalzp.compression.options.Options

import scala.scalajs.js.typedarray.{Int16Array, Int32Array}

abstract class Common(options: Options) {
  import Common._
  // Options
  private val lzpLowContextLength = options.lzpLowContextLength
  private val lzpLowMaskSize = options.lzpLowMaskSize
  private val lzpHighContextLength = options.lzpHighContextLength
  private val lzpHighMaskSize = options.lzpHighMaskSize
  private val literalCoderOrder = options.literalCoderOrder
  private val literalCoderInit = options.literalCoderInit
  private val literalCoderStep = options.literalCoderStep
  private val literalCoderLimit = options.literalCoderLimit
  // Lempel-Ziv Predictive
  protected val onlyLowLzp = lzpLowContextLength == lzpHighContextLength &&
    lzpLowMaskSize == lzpHighMaskSize
  private def lzpLowCount =
    1 << lzpLowMaskSize
  private def lzpHighCount =
    1 << lzpHighMaskSize
  private val lzpLowMask = lzpLowCount - 1
  private val lzpHighMask = lzpHighCount - 1
  private val lzpLowTable = arrayOfShorts(lzpLowCount, 0xffb5.toShort)
  private val lzpHighTable =
    if (onlyLowLzp) {
      null
    } else {
      arrayOfShorts(lzpHighCount, 0xffb5.toShort)
    }
  // Literal coder
  private val literalCoderContextMaskSize = 8 * literalCoderOrder
  private val literalCoderContextMask = (1 << literalCoderContextMaskSize) - 1
  protected val rangesSingle = arrayOfShorts(
    1 << literalCoderContextMaskSize + 8, literalCoderInit.toShort)
  protected val rangesGrouped = arrayOfShorts(
    1 << literalCoderContextMaskSize + 4, (literalCoderInit << 4).toShort)
  protected val rangesTotal = arrayOfShorts(
    1 << literalCoderContextMaskSize, (literalCoderInit << 8).toShort)
  private var recentCost = 8 << CostScale + 14
  // Contexts and hashes
  protected var lastLiteralCoderContext = 0
  private var context = 0l
  private var hashLow = 0
  private var hashHigh = 0
  // Adaptive probability map
  private val apmLowTable = arrayOfShorts(16 * 256, 0x4000.toShort)
  private val apmHighTable =
    if (onlyLowLzp) {
      null
    } else {
      arrayOfShorts(16 * 256, 0x4000.toShort)
    }

  // <editor-fold defaultstate="collapsed" desc="Contexts and hashes">
  protected def updateContext(input: Int): Unit = {
    context <<= 8
    context |= input
  }

  protected def computeLiteralCoderContext(): Unit =
    lastLiteralCoderContext = (context & literalCoderContextMask).toInt

  protected def computeHashesOnlyLowLzp(): Unit = {
    var localContext = context >>> 8
    var hash = precomputedHashes((context & 0xff).toInt)
    var finished = false
    var i = 1
    while (!finished) {
      hash ^= (localContext & 0xff).toInt
      localContext >>= 8
      i += 1
      if (i == lzpLowContextLength) {
        finished = true
      } else {
        hash *= 16777619
      }
    }
    hashLow = hash & lzpLowMask
  }

  protected def computeHashes(): Unit = {
    var localContext = context >>> 8
    var hash = precomputedHashes((context & 0xff).toInt)
    var finished = false
    var i = 1
    while (!finished) {
      hash ^= (localContext & 0xff).toInt
      localContext >>>= 8
      i += 1
      if (i == lzpLowContextLength) {
        finished = true
      } else {
        hash *= 16777619
      }
    }
    hashLow = hash & lzpLowMask
    while (i < lzpHighContextLength) {
      i += 1
      hash *= 16777619
      hash ^= (localContext & 0xff).toInt
      localContext >>>= 8
    }
    hashHigh = hash & lzpHighMask
  }// </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Calculating states">
  private def getNextState(state: Int, matched: Boolean): Int =
    stateTable(state * 2 + (if (matched) 1 else 0)) & 0xff
  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Lempel-Ziv Predictive stuff">
  protected def getLzpStateLow: Int =
    (lzpLowTable(hashLow) >> 8) & 0xff

  protected def getLzpStateHigh: Int =
    (lzpHighTable(hashHigh) >> 8) & 0xff

  protected def getLzpPredictedSymbolLow: Int =
    lzpLowTable(hashLow) & 0xff

  protected def getLzpPredictedSymbolHigh: Int =
    lzpHighTable(hashHigh) & 0xff

  protected def updateLzpStateLow(lzpStateLow: Int, input: Int,
    matched: Boolean): Unit = {
    lzpLowTable(hashLow) =
      ((getNextState(lzpStateLow, matched) << 8) + input).toShort
  }

  protected def updateLzpStateHigh(lzpStateHigh: Int, input: Int,
    matched: Boolean): Unit = {
    lzpHighTable(hashHigh) =
      ((getNextState(lzpStateHigh, matched) << 8) + input).toShort
  }// </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Adaptive prob. map stuff">
  private var historyLow = 0
  private var historyHigh = 0
  private val historyLowMask = 15
  private val historyHighMask = 15

  protected def getApmLow(state: Int): Int =
    apmLowTable((historyLow << 8) + state)

  protected def getApmHigh(state: Int): Int =
    apmHighTable((historyHigh << 8) + state)

  protected def updateApmHistoryLow(matched: Boolean): Unit =
    historyLow = (historyLow << 1) + (if (matched) 0 else 1) & historyLowMask

  protected def updateApmHistoryHigh(matched: Boolean): Unit =
    historyHigh = (historyHigh << 1) + (if (matched) 0 else 1) & historyHighMask

  private def apmLowAccumulate(index: Int, component: Int): Unit =
    apmLowTable(index) = (apmLowTable(index) + component).toShort

  private def apmHighAccumulate(index: Int, component: Int): Unit =
    apmHighTable(index) = (apmHighTable(index) + component).toShort

  protected def updateApmLow(state: Int, matched: Boolean): Unit = {
    val index = (historyLow << 8) + state
    apmLowAccumulate(index,
      if (matched) {
        ((1 << 15) - apmLowTable(index)) >> 7
      } else {
        -(apmLowTable(index) >> 7)
      }
    )
    updateApmHistoryLow(matched)
  }

  protected def updateApmHigh(state: Int, matched: Boolean): Unit = {
    val index = (historyHigh << 8) + state
    apmHighAccumulate(index,
      if (matched) {
        ((1 << 15) - apmHighTable(index)) >> 7
      } else {
        -(apmHighTable(index) >> 7)
      }
    )
    updateApmHistoryHigh(matched)
  }// </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Literal coder stuff">
  private def rangesSingleAccumulate(index: Int, component: Int): Unit =
    rangesSingle(index) = (rangesSingle(index) + component).toShort

  protected def rangesGroupedAccumulate(index: Int, component: Int): Unit =
    rangesGrouped(index) = (rangesGrouped(index) + component).toShort

  private def rangesTotalAccumulate(index: Int, component: Int): Unit =
    rangesTotal(index) = (rangesTotal(index) + component).toShort

  private def rescaleLiteralCoder(): Unit = {
    for (indexCurrent <- (lastLiteralCoderContext << 8) until
      (lastLiteralCoderContext + 1 << 8)) {
      rangesSingleAccumulate(indexCurrent, -(rangesSingle(indexCurrent) >> 1))
    }
    var totalFrequency = 0
    for (groupCurrent <- (lastLiteralCoderContext << 4) until
      (lastLiteralCoderContext + 1 << 4)) {
      var groupFrequency = 0
      for (indexCurrent <- (groupCurrent << 4) until (groupCurrent + 1 << 4)) {
        groupFrequency += rangesSingle(indexCurrent)
      }
      rangesGrouped(groupCurrent) = groupFrequency.toShort
      totalFrequency += groupFrequency
    }
    rangesTotal(lastLiteralCoderContext) = totalFrequency.toShort
  }

  protected def updateLiteralCoder(index: Int): Unit = {
    rangesSingleAccumulate(index, literalCoderStep)
    rangesGroupedAccumulate(index >> 4, literalCoderStep)
    rangesTotalAccumulate(lastLiteralCoderContext, literalCoderStep)
    if (rangesTotal(lastLiteralCoderContext) > literalCoderLimit) {
      rescaleLiteralCoder()
    }
  }

  protected def shouldUseFixedProbabilities: Boolean =
    recentCost > (8 << CostScale + 14)

  protected def updateRecentCost(symbolFrequency: Int,
    totalFrequency: Int): Unit = {
    recentCost -= recentCost >> CostScale
    recentCost += Lg2.nLog2(totalFrequency)
    recentCost -= Lg2.nLog2(symbolFrequency)

  }
  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="Literal coder stuff">
  private def arrayOfShorts(length: Int, value: Short): Int16Array = {
    val array = new Int16Array(length)
    (0 until length).foreach(array(_) = value)
    array
  }
  // </editor-fold>
}

object Common {
  // Literal coder
  private val CostScale = 7
  // Contexts and hashes
  private val precomputedHashes = {
    val array = new Int32Array(256)
    (0 until 256).foreach { i =>
      var hash = -2128831035
      hash *= 16777619
      hash ^= i
      hash *= 16777619
      array(i) = hash
    }
    array
  }
  // Calculating states
  private val stateTable = FsmGenerator.stateTable
}
