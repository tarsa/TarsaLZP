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
package pl.tarsa.tarsalzp.prelude

import java.io.{InputStream, OutputStream}

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

object Streams {

  class ArrayInputStream(array: Uint8Array) extends InputStream {
    private var position = 0

    override def read(): Int = {
      if (position == array.length) {
        -1
      } else {
        val result = array(position)
        position += 1
        result
      }
    }
  }

  class ChunksArrayOutputStream extends OutputStream {
    private val chunksArray = js.Array[Chunk]()
    private var chunk = new Chunk()

    override def write(value: Int): Unit = {
      if (chunk.isFull) {
        chunksArray.push(chunk)
        chunk = new Chunk()
      }
      chunk.write(value.toByte)
    }

    def toBlob(blobType: String = "example/binary"): dom.Blob = {
      val chunks = new js.Array[js.Any]()
      chunksArray.foreach(chunk => chunks.push(chunk.truncatedBuffer))
      chunks.push(chunk.truncatedBuffer)
      new dom.Blob(chunks,
        dom.raw.BlobPropertyBag(s"{type: '$blobType'}"))
    }
  }
}
