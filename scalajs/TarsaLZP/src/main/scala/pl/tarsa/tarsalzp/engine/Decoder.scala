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
package pl.tarsa.tarsalzp.engine

import java.io.{IOException, InputStream, OutputStream}

import pl.tarsa.tarsalzp.options.Options

class Decoder(
  inputStream: InputStream,
  outputStream: OutputStream,
  options: Options
) extends Common(options) {

  private var rcBuffer = 0
  private var rcRange = 0
  private var started = false
  private var nextHighBit = 0

  private def inputByte(): Int = {
    val inputByte = inputStream.read()
    if (inputByte == -1) {
      throw new IOException("Unexpected end of file")
    }
    val currentByte = (inputByte >> 1) + (nextHighBit << 7)
    nextHighBit = inputByte & 1
    currentByte
  }

  private def init(): Unit = {
    for (i <- 0 until 4) {
      rcBuffer = (rcBuffer << 8) + inputByte()
    }
    rcRange = 0x7fffffff
    started = true
  }

  private def normalize(): Unit = {
    while (rcRange < 0x00800000) {
      rcBuffer = (rcBuffer << 8) + inputByte()
      rcRange <<= 8
    }
  }

  private def decodeFlag(probability: Int): Boolean = {
    normalize()
    val rcHelper = (rcRange >> 15) * probability
    if (rcHelper > rcBuffer) {
      rcRange = rcHelper
      true
    } else {
      rcRange -= rcHelper
      rcBuffer -= rcHelper
      false
    }
  }

  private def decodeSkewed(): Boolean = {
    normalize()
    if (rcBuffer < rcRange - 1) {
      rcRange -= 1
      true
    } else {
      rcBuffer = 0
      rcRange = 1
      false
    }
  }

  private def decodeSingleOnlyLowLzp(): Int = {
    computeHashesOnlyLowLzp()
    val lzpStateLow = getLzpStateLow
    val predictedSymbolLow = getLzpPredictedSymbolLow
    val modelLowFrequency = getApmLow(lzpStateLow)
    val matchLow = decodeFlag(modelLowFrequency)
    updateApmLow(lzpStateLow, matchLow)
    val nextSymbol =
      if (matchLow) {
        predictedSymbolLow
      } else {
        decodeSymbol(predictedSymbolLow)
      }
    updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
    updateContext(nextSymbol)
    nextSymbol
  }

  private def decodeSingle(): Int = {
    computeHashes()
    val lzpStateLow = getLzpStateLow
    val predictedSymbolLow = getLzpPredictedSymbolLow
    val modelLowFrequency = getApmLow(lzpStateLow)
    val lzpStateHigh = getLzpStateHigh
    val predictedSymbolHigh = getLzpPredictedSymbolHigh
    val modelHighFrequency = getApmHigh(lzpStateHigh)
    var nextSymbol = 0
    if (modelLowFrequency >= modelHighFrequency) {
      val matchLow = decodeFlag(modelLowFrequency)
      updateApmLow(lzpStateLow, matchLow)
      nextSymbol =
        if (matchLow) {
          predictedSymbolLow
        } else {
          decodeSymbol(predictedSymbolLow)
        }
      updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
      val matchHigh = nextSymbol == predictedSymbolHigh
      updateApmHistoryHigh(matchHigh)
      updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
    } else {
      val matchHigh = decodeFlag(modelHighFrequency)
      updateApmHigh(lzpStateHigh, matchHigh)
      nextSymbol =
        if (matchHigh) {
          predictedSymbolHigh
        } else {
          decodeSymbol(predictedSymbolHigh)
        }
      updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
      val matchLow = nextSymbol == predictedSymbolLow
      updateApmHistoryLow(matchLow)
      updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
    }
    updateContext(nextSymbol)
    nextSymbol
  }

  private def decodeSymbol(mispredictedSymbol: Int): Int = {
    normalize()
    computeLiteralCoderContext()
    var index = 0
    var nextSymbol = 0
    if (!shouldUseFixedProbabilities) {
      val mispredictedSymbolFrequency =
        rangesSingle((lastLiteralCoderContext << 8) + mispredictedSymbol)
      rcRange /=
        rangesTotal(lastLiteralCoderContext) - mispredictedSymbolFrequency
      rangesSingle((lastLiteralCoderContext << 8) + mispredictedSymbol) = 0
      rangesGroupedAccumulate(((lastLiteralCoderContext << 8) +
        mispredictedSymbol) >> 4, -mispredictedSymbolFrequency)
      var rcHelper = rcBuffer / rcRange
      val cumulativeFrequency = rcHelper
      index = lastLiteralCoderContext << 4
      while (rcHelper >= rangesGrouped(index)) {
        rcHelper -= rangesGrouped(index)
        index += 1
      }
      index <<= 4
      while (rcHelper >= rangesSingle(index)) {
        rcHelper -= rangesSingle(index)
        index += 1
      }
      rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange
      rcRange *= rangesSingle(index)
      nextSymbol = index & 0xff
      rangesSingle((lastLiteralCoderContext << 8) + mispredictedSymbol) =
        mispredictedSymbolFrequency
      rangesGroupedAccumulate(((lastLiteralCoderContext << 8) +
        mispredictedSymbol) >> 4, mispredictedSymbolFrequency)
    } else {
      rcRange /= 255
      val rcHelper = rcBuffer / rcRange
      rcBuffer -= rcHelper * rcRange
      nextSymbol = rcHelper + (if (rcHelper >= mispredictedSymbol) 1 else 0)
      index = (lastLiteralCoderContext << 8) + nextSymbol
    }
    updateRecentCost(rangesSingle(index), rangesTotal(lastLiteralCoderContext))
    updateLiteralCoder(index)
    nextSymbol
  }

  def decode(limit: Long): Long = {
    if (!started) {
      init()
    }
    var endReached = false
    var result = limit
    for (processed <- 0l until limit if !endReached) {
      if (decodeSkewed()) {
        val symbol =
          if (onlyLowLzp) {
            decodeSingleOnlyLowLzp()
          } else {
            decodeSingle()
          }
        outputStream.write(symbol)
      } else {
        result = processed
        endReached = true
      }
    }
    result
  }
}
