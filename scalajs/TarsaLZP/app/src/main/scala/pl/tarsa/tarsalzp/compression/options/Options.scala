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
package pl.tarsa.tarsalzp.compression.options

case class Options(
    lzpLowContextLength: Int,
    lzpLowMaskSize: Int,
    lzpHighContextLength: Int,
    lzpHighMaskSize: Int,
    literalCoderOrder: Int,
    literalCoderInit: Int,
    literalCoderStep: Int,
    literalCoderLimit: Int
) {
  def isValid: Boolean = {
    lzpLowContextLength > literalCoderOrder &&
    lzpLowContextLength <= lzpHighContextLength &&
    lzpHighContextLength <= 8 &&
    lzpLowMaskSize >= 15 &&
    lzpLowMaskSize <= 30 &&
    lzpHighMaskSize >= 15 &&
    lzpHighMaskSize <= 30 &&
    literalCoderOrder >= 1 &&
    literalCoderOrder <= 2 &&
    literalCoderInit >= 1 &&
    literalCoderInit <= 127 &&
    literalCoderStep >= 1 &&
    literalCoderStep <= 127 &&
    literalCoderLimit >= literalCoderInit * 256 &&
    literalCoderLimit <= 32767 - literalCoderStep
  }

  def toPacked: Long = {
    (lzpLowContextLength.toLong << 56) +
      (lzpLowMaskSize.toLong << 48) +
      (lzpHighContextLength.toLong << 40) +
      (lzpHighMaskSize.toLong << 32) +
      ((literalCoderOrder - 1).toLong << 31) +
      (literalCoderInit << 24) +
      (literalCoderStep << 16) +
      literalCoderLimit
  }

  def validated: Option[Options] = {
    if (isValid) {
      Some(this)
    } else {
      None
    }
  }

  def prettyFormat: String = {
    s"""|Options:
        |Low Context Length: $lzpLowContextLength
        |Low Mask Size: $lzpLowMaskSize
        |High Context Length: $lzpHighContextLength
        |High Mask Size: $lzpHighMaskSize
        |Literal Coder Order: $literalCoderOrder
        |Literal Coder Init: $literalCoderInit
        |Literal Coder Step: $literalCoderStep
        |Literal Coder Limit: $literalCoderLimit
        |""".stripMargin
  }
}

object Options {
  val default: Options = {
    Options(
      lzpLowContextLength = 4,
      lzpLowMaskSize = 24,
      lzpHighContextLength = 8,
      lzpHighMaskSize = 27,
      literalCoderOrder = 2,
      literalCoderInit = 1,
      literalCoderStep = 60,
      literalCoderLimit = 30000
    )
  }

  def fromPacked(packed: Long): Options = {
    Options(
      (packed >> 56).toInt & 0xff,
      (packed >> 48).toInt & 0xff,
      (packed >> 40).toInt & 0xff,
      (packed >> 32).toInt & 0xff,
      ((packed >> 31).toInt & 0x1) + 1,
      (packed >> 24).toInt & 0x7f,
      (packed >> 16).toInt & 0xff,
      packed.toInt & 0xffff
    )
  }

  sealed abstract class Updater private (val run: Options => Options)

  object Updater {
    case class NewLzpLowContextLength(value: Int)
        extends Updater(_.copy(lzpLowContextLength = value))

    case class NewLzpLowMaskSize(value: Int)
        extends Updater(_.copy(lzpLowMaskSize = value))

    case class NewLzpHighContextLength(value: Int)
        extends Updater(_.copy(lzpHighContextLength = value))

    case class NewLzpHighMaskSize(value: Int)
        extends Updater(_.copy(lzpHighMaskSize = value))

    case class NewLiteralCoderOrder(value: Int)
        extends Updater(_.copy(literalCoderOrder = value))

    case class NewLiteralCoderInit(value: Int)
        extends Updater(_.copy(literalCoderInit = value))

    case class NewLiteralCoderStep(value: Int)
        extends Updater(_.copy(literalCoderStep = value))

    case class NewLiteralCoderLimit(value: Int)
        extends Updater(_.copy(literalCoderLimit = value))
  }
}
