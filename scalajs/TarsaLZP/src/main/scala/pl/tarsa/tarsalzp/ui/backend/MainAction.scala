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
package pl.tarsa.tarsalzp.ui.backend

import diode.Action
import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.util.RafAction

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

sealed trait MainAction extends Action

sealed trait IdleStateAction extends MainAction

case class UpdateOptions(modify: Options => Options) extends IdleStateAction

case class ChangeChunkSize(newChunkSize: Int) extends IdleStateAction

case class ChangedMode(newMode: ProcessingMode) extends IdleStateAction

case class SelectedFile(fileOpt: Option[dom.File]) extends IdleStateAction

case object LoadFile extends IdleStateAction

case class LoadingFinished(inputBufferOpt: Option[ArrayBuffer])
  extends IdleStateAction

case object StartProcessing extends IdleStateAction

case object SaveFile extends IdleStateAction

sealed trait CodingInProgressAction extends MainAction

case object ContinueProcessing extends CodingInProgressAction

case class ChunkProcessed(measurements: ChunkCodingMeasurement)
  extends CodingInProgressAction with RafAction

case class ProcessingFinished(endTime: js.Date) extends CodingInProgressAction
  with RafAction
