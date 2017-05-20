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

import scala.scalajs.js.typedarray.Int8Array

object Lg2 {
  private val lgLut = {
    val array = new Int8Array(256)
    array(0) = -1
    for (i <- 1 until 256) {
      array(i) = (1 + array(i / 2)).toByte
    }
    array
  }

  private def iLog2(value: Int): Byte = {
    if (value >= 256 && value < 65536) {
      (8 + lgLut(value >> 8)).toByte
    } else if (value < 256) {
      lgLut(value)
    } else {
      (16 + iLog2(value >>> 16)).toByte
    }
  }

  /**
    * Approximate logarithm base 2 scaled by 2^14, Works only for positive
    * values lower than 2^15.
    */
  def nLog2(value: Int): Int = {
    val ilog = iLog2(value)
    val norm = value << 14 - ilog
    (ilog - 1 << 14) + norm
  }
}
