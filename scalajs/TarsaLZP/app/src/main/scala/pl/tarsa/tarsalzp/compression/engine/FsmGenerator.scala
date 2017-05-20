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

object FsmGenerator {
  val stateTable = {
    val states = new Int8Array(512)
    val LimitX = 20
    val LimitY = 20
    var p = 0
    val freqMask = Array.fill[Int]((LimitX + 1) * (LimitY + 1) * 3 * 3)(-1)

    def initStates(xIn: Int, yIn: Int, h1: Int, h0: Int): Byte = {
      val x = Math.min(xIn, LimitX)
      val y = Math.min(yIn, LimitY)
      val index = ((y * (LimitX + 1) + x) * 3 + h1) * 3 + h0
      if (freqMask(index) == -1) {
        freqMask(index) = p
        val c = p
        p += 1
        states(c * 2 + 0) = initStates(repeated(x, y), opposite(x, y), h0, 0)
        states(c * 2 + 1) = initStates(opposite(y, x), repeated(y, x), h0, 1)
      }
      freqMask(index).toByte
    }

    def repeated(a: Int, b: Int): Int = {
      if (b > 0 && divisor(a, b) > 1200) {
        (a + 1) * 1950 / divisor(a, b)
      } else {
        a + 1
      }
    }

    def opposite(a: Int, b: Int): Int = {
      if (b > 0 && divisor(a, b) > 1200) {
        b * 1950 / divisor(a, b)
      } else {
        b
      }
    }

    def divisor(a: Int, b: Int): Int =
      (Lg2.nLog2(b) >> 3) + (Lg2.nLog2(1950) >> 3) - (12 << 11)

    initStates(0, 0, 2, 2)

    states
  }
}
