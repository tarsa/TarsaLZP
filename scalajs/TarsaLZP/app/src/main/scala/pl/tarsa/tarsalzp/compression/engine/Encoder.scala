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

import java.io.{InputStream, OutputStream}

import pl.tarsa.tarsalzp.compression.options.Options


class Encoder(
  inputStream: InputStream,
  outputStream: OutputStream,
  options: Options
) extends Common(options) {

  private var rcBuffer = 0
  private var rcRange = 0x7fffffff
  private var xFFRunLength = 0
  private var lastOutputByte = 0
  private var delay = false
  private var carry = false

  private def outputByte(octet: Int): Unit = {
    if (octet != 0xff || carry) {
      if (delay) {
        outputStream.write(lastOutputByte + (if (carry) 1 else 0))
      }
      while (xFFRunLength > 0) {
        xFFRunLength -= 1
        outputStream.write(if (carry) 0x00 else 0xff)
      }
      lastOutputByte = octet
      delay = true
      carry = false
    } else {
      xFFRunLength += 1
    }
  }

  private def normalize(): Unit = {
    while (rcRange < 0x00800000) {
      outputByte(rcBuffer >> 23)
      rcBuffer = (rcBuffer << 8) & 0x7fffffff
      rcRange <<= 8
    }
  }

  private def addWithCarry(cumulativeExclusiveFraction: Int): Unit = {
    rcBuffer += cumulativeExclusiveFraction
    if (rcBuffer < 0) {
      carry = true
      rcBuffer &= 0x7fffffff
    }
  }

  private def encodeFlag(probability: Int, matched: Boolean): Unit = {
    normalize()
    val rcHelper = (rcRange >> 15) * probability
    if (matched) {
      rcRange = rcHelper
    } else {
      addWithCarry(rcHelper)
      rcRange -= rcHelper
    }
  }

  private def encodeSkewed(flag: Boolean): Unit = {
    normalize()
    if (flag) {
      rcRange -= 1
    } else {
      addWithCarry(rcRange - 1)
      rcRange = 1
    }
  }

  private def encodeSingleOnlyLowLzp(nextSymbol: Int): Unit = {
    computeHashesOnlyLowLzp()
    val lzpStateLow = getLzpStateLow
    val predictedSymbolLow = getLzpPredictedSymbolLow
    val modelLowFrequency = getApmLow(lzpStateLow)
    val matchLow = nextSymbol == predictedSymbolLow
    encodeFlag(modelLowFrequency, matchLow)
    updateApmLow(lzpStateLow, matchLow)
    updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
    if (!matchLow) {
      encodeSymbol(nextSymbol, predictedSymbolLow)
    }
    updateContext(nextSymbol)
  }

  private def encodeSingle(nextSymbol: Int): Unit = {
    computeHashes()
    val lzpStateLow = getLzpStateLow
    val predictedSymbolLow = getLzpPredictedSymbolLow
    val modelLowFrequency = getApmLow(lzpStateLow)
    val lzpStateHigh = getLzpStateHigh
    val predictedSymbolHigh = getLzpPredictedSymbolHigh
    val modelHighFrequency = getApmHigh(lzpStateHigh)
    if (modelLowFrequency >= modelHighFrequency) {
      val matchHigh = nextSymbol == predictedSymbolHigh
      updateApmHistoryHigh(matchHigh)
      updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
      val matchLow = nextSymbol == predictedSymbolLow
      encodeFlag(modelLowFrequency, matchLow)
      updateApmLow(lzpStateLow, matchLow)
      updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
      if (!matchLow) {
        encodeSymbol(nextSymbol, predictedSymbolLow)
      }
    } else {
      val matchLow = nextSymbol == predictedSymbolLow
      updateApmHistoryLow(matchLow)
      updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
      val matchHigh = nextSymbol == predictedSymbolHigh
      encodeFlag(modelHighFrequency, matchHigh)
      updateApmHigh(lzpStateHigh, matchHigh)
      updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
      if (!matchHigh) {
        encodeSymbol(nextSymbol, predictedSymbolHigh)
      }
    }
    updateContext(nextSymbol)
  }

  private def encodeSymbol(nextSymbol: Int, mispredictedSymbol: Int): Unit = {
    normalize()
    computeLiteralCoderContext()
    val index = (lastLiteralCoderContext << 8) + nextSymbol
    if (!shouldUseFixedProbabilities) {
      var cumulativeExclusiveFrequency = 0
      val symbolGroup = index >> 4
      for (indexPartial <- (lastLiteralCoderContext << 4) until symbolGroup) {
        cumulativeExclusiveFrequency += rangesGrouped(indexPartial)
      }
      for (indexPartial <- (symbolGroup << 4) until index) {
        cumulativeExclusiveFrequency += rangesSingle(indexPartial)
      }
      val mispredictedSymbolFrequency =
        rangesSingle((lastLiteralCoderContext << 8) + mispredictedSymbol)
      if (nextSymbol > mispredictedSymbol) {
        cumulativeExclusiveFrequency -= mispredictedSymbolFrequency
      }
      val rcHelper = rcRange /
        (rangesTotal(lastLiteralCoderContext) - mispredictedSymbolFrequency)
      addWithCarry(rcHelper * cumulativeExclusiveFrequency)
      rcRange = rcHelper * rangesSingle(index)
    } else {
      rcRange /= 255
      addWithCarry(rcRange *
        (nextSymbol - (if (nextSymbol > mispredictedSymbol) 1 else 0)))
    }
    updateRecentCost(rangesSingle(index), rangesTotal(lastLiteralCoderContext))
    updateLiteralCoder(index)
  }

  def flush(): Unit = {
    for (i <- 0 until 5) {
      outputByte((rcBuffer >> 23) & 0xff)
      rcBuffer <<= 8
    }
  }

  def encode(limit: Int): Int = {
    var endReached = false
    var result = limit
    for (i <- 0 until limit if !endReached) {
      val symbol = inputStream.read()
      endReached = symbol == -1
      encodeSkewed(!endReached)
      if (endReached) {
        result = i
      } else {
        if (onlyLowLzp) {
          encodeSingleOnlyLowLzp(symbol)
        } else {
          encodeSingle(symbol)
        }
      }
    }
    result
  }
}
