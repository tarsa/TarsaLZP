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
package _infrastructure.data

import org.scalajs.dom
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.backend.MainModel
import pl.tarsa.tarsalzp.ui.backend.MainModel.{
  CodingInProgressViewData,
  IdleStateViewData
}
import pl.tarsa.tarsalzp.ui.backend.ProcessingMode.EncodingMode

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

object Models {
  private val initialTaskViewData =
    IdleStateViewData(EncodingMode, None, None, loadingInProgress = false)

  val initialModel =
    MainModel(Options.default, None, 345, initialTaskViewData)

  val afterFileSelection: MainModel = initialModel.copy(
    chosenFileOpt = Some(new dom.Blob().asInstanceOf[dom.File])
  )

  val duringFileLoading: MainModel = afterFileSelection.copy(
    taskViewData = initialTaskViewData.copy(loadingInProgress = true)
  )

  val withLoadedFile: MainModel = initialModel.copy(
      taskViewData = initialTaskViewData.copy(
          inputArrayOpt = Some(new Uint8Array(3))))

  val afterStartedEncoding: MainModel = withLoadedFile.copy(
      taskViewData = CodingInProgressViewData(EncodingMode, new Uint8Array(3),
        3, 0, 0, new js.Date(), Seq.empty))
}
