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

import java.io.{IOException, InputStream, OutputStream}

import pl.tarsa.tarsalzp.compression.options.Options

object Coder {
  private val HeaderValue = 2345174324078614718L

  def checkHeader(inputStream: InputStream): Unit = {
    val header = readLong(inputStream)
    if (header != HeaderValue) {
      throw new IOException(
        "Wrong file header. Probably not a compressed file.")
    }
  }

  def getOptions(inputStream: InputStream): Options = {
    val packedOptions = readLong(inputStream)
    Options.fromPacked(packedOptions).validated.getOrElse(
      throw new IllegalArgumentException("Invalid compression options"))
  }

  def startDecoder(inputStream: InputStream,
    outputStream: OutputStream): Decoder = {
    checkHeader(inputStream)
    val options = getOptions(inputStream)
    new Decoder(inputStream, outputStream, options)
  }

  def startEncoder(inputStream: InputStream, outputStream: OutputStream,
    options: Options): Encoder = {
    writeLong(outputStream, HeaderValue)
    val packedOptions = options.toPacked
    writeLong(outputStream, packedOptions)
    new Encoder(inputStream, outputStream, options)
  }

  private def readLong(inputStream: InputStream): Long = {
    Iterator.continually(inputStream.read()).take(8).foldLeft(0L)(
      (accumulator, inputByte) =>
        if (inputByte == -1) {
          throw new IOException("Unexpected end of file")
        } else {
          (accumulator << 8) + inputByte
        }
    )
  }

  private def writeLong(outputStream: OutputStream, value: Long): Unit = {
    for (i <- 0 until 8) {
      outputStream.write((value << i * 8 >>> 56).toInt & 0xff)
    }
  }
}
