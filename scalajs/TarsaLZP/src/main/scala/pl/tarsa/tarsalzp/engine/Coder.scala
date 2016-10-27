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

object Coder {
  private val HeaderValue = 2345174324078614718l


  private trait Callback {
    def progressChanged(processedSymbols: Long): Unit
  }

  def getOptions(inputStream: InputStream): Options = {
    var header = 0l
    for (i <- 0 until 8) {
      header <<= 8
      val inputByte = inputStream.read()
      if (inputByte == -1) {
        throw new IOException("Unexpected end of file")
      }
      header |= inputByte
    }
    if (header != HeaderValue) {
      throw new IOException(
        "Wrong file header. Probably not a compressed file.")
    }
    getOptionsHeaderless(inputStream)
  }

  private def getOptionsHeaderless(inputStream: InputStream): Options = {
    var packedOptions = 0l
    for (i <- 0 until 8) {
      packedOptions <<= 8
      val inputByte = inputStream.read()
      if (inputByte == -1) {
        throw new IOException("Unexpected end of file")
      }
      packedOptions |= inputByte
    }
    val result = Options.fromPacked(packedOptions)
    if (result.isValid) {
      result
    } else {
      throw new IllegalArgumentException("Invalid compression options")
    }
  }

  private def checkInterval(intervalLength: Long): Unit = {
    if (intervalLength <= 0) {
      throw new IllegalArgumentException(
        "Interval length has to be positive")
    }
  }

  def decode(inputStream: InputStream, outputStream: OutputStream,
    callback: Callback, intervalLength: Long): Unit = {
    checkInterval(intervalLength)
    val options = getOptions(inputStream)
    decodeRaw(inputStream, outputStream, callback, intervalLength, options)
  }

  private def decodeRaw(inputStream: InputStream, outputStream: OutputStream,
    callback: Callback, intervalLength: Long, options: Options): Unit = {
    checkInterval(intervalLength)
    val decoder = new Decoder(inputStream, outputStream, options)
    var totalAmountProcessed = 0l
    var shouldContinue = true
    while (shouldContinue) {
      val currentAmountProcessed = decoder.decode(intervalLength)
      totalAmountProcessed += currentAmountProcessed
      if (callback != null) {
        callback.progressChanged(totalAmountProcessed)
      }
      shouldContinue = currentAmountProcessed == intervalLength
    }
  }

  def encode(inputStream: InputStream, outputStream: OutputStream,
    callback: Callback, intervalLength: Long, options: Options): Unit = {
    checkInterval(intervalLength)
    var header = HeaderValue
    for (i <- 0 until 8) {
      outputStream.write((header >>> 56).toInt & 0xff)
      header <<= 8
    }
    var packedOptions = options.toPacked
    for (i <- 0 until 8) {
      outputStream.write((packedOptions >>> 56).toInt & 0xff)
      packedOptions <<= 8
    }
    encodeRaw(inputStream, outputStream, callback, intervalLength, options)
  }

  private def encodeRaw(inputStream: InputStream, outputStream: OutputStream,
    callback: Callback, intervalLength: Long, options: Options): Unit = {
    checkInterval(intervalLength)
    val encoder = new Encoder(inputStream, outputStream, options)
    var totalAmountProcessed = 0l
    var shouldContinue = true
    while (shouldContinue) {
      val currentAmountProcessed = encoder.encode(intervalLength)
      totalAmountProcessed += currentAmountProcessed
      if (callback != null) {
        callback.progressChanged(totalAmountProcessed)
      }
      shouldContinue = currentAmountProcessed == intervalLength
    }
    encoder.flush()
  }
}
