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
package pl.tarsa.tarsalzp.prelude

import akka.util.HashCode

import scala.scalajs.js

class WrappedTypedArray(val array: js.typedarray.Uint8Array) {
  override def equals(other: scala.Any): Boolean = {
    other match {
      case other: WrappedTypedArray =>
        array.toIterator.sameElements(other.array.toIterator)
      case _ =>
        false
    }
  }

  override def hashCode(): Int =
    array.foldLeft(HashCode.SEED)(HashCode.hash(_, _))
}
