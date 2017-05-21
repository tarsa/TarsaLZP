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

import java.io.{InputStream, OutputStream}

import org.scalajs.dom

import scala.scalajs.js

object Streams {

  class ArrayInputStream(val array: js.typedarray.Uint8Array)
      extends InputStream {
    private var _position = 0

    def position: Int =
      _position

    override def read(): Int = {
      if (_position == array.length) {
        -1
      } else {
        val result = array(_position)
        _position += 1
        result
      }
    }
  }

  class ChunksArrayOutputStream extends OutputStream with BlobSource {
    private var cumulativeExclusiveChunksArraySize = 0
    private val chunksArray = js.Array[Chunk]()
    private var chunk = new Chunk()

    def position: Int =
      cumulativeExclusiveChunksArraySize + chunk.position

    override def write(value: Int): Unit = {
      if (chunk.isFull) {
        chunksArray.push(chunk)
        cumulativeExclusiveChunksArraySize += chunk.position
        chunk = new Chunk()
      }
      chunk.write(value.toByte)
    }

    override def toBlob: dom.Blob = {
      val chunks = new js.Array[js.Any]()
      chunksArray.foreach { chunk =>
        chunks.push(chunk.truncatedArray.raw.buffer)
      }
      chunks.push(chunk.truncatedArray.raw.buffer)
      new dom.Blob(chunks, dom.raw.BlobPropertyBag(s"example/binary"))
    }

    override def toIterator: Iterator[Byte] = {
      chunksArray.toIterator.flatMap(_.toIterator) ++ chunk.toIterator
    }
  }
}
