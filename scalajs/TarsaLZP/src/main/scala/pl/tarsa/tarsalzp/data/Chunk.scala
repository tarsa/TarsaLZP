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
package pl.tarsa.tarsalzp.data

import scala.scalajs.js

class Chunk(chunkSize: Int = 64 * 1024) {
  private val array = {
    val buffer = new js.typedarray.ArrayBuffer(chunkSize)
    val typedArray = new js.typedarray.Uint8Array(buffer)
    new WrappedTypedArray(typedArray)
  }

  private var _position = 0

  def position: Int =
    _position

  def write(value: Byte): Unit = {
    array.raw(_position) = value
    _position += 1
  }

  def isFull: Boolean =
    _position == chunkSize

  def truncatedArray: WrappedTypedArray = {
    val buffer = array.raw.buffer.slice(0, _position)
    val typedArray = new js.typedarray.Uint8Array(buffer)
    new WrappedTypedArray(typedArray)
  }

  final override def equals(that: scala.Any): Boolean = {
    that match {
      case that: Chunk =>
        truncatedArray.equals(that.truncatedArray)
      case _ =>
        false
    }
  }

  final override def hashCode(): Int =
    truncatedArray.hashCode()

  def toIterator: Iterator[Byte] =
    Iterator.tabulate(_position)(array.raw(_).toByte)
}
