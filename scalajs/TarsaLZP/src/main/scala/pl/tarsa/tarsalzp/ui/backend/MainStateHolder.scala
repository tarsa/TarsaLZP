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

import diode.Circuit
import diode.react.ReactConnector
import pl.tarsa.tarsalzp.compression.options.Options
import pl.tarsa.tarsalzp.ui.util.RafTimestampHandler

class MainStateHolder
  extends Circuit[MainModel]
    with ReactConnector[MainModel] {
  override protected def initialModel = {
    MainModel(
      ViewData(Options.default, None, 1234567,
        IdleStateViewData(EncodingMode, None, None, loadingInProgress = false)),
      IdleStateProcessingData
    )
  }

  override protected val actionHandler = {
    composeHandlers(
      new MainActionHandler(zoomRW(identity)((_, m) => m)),
      new RafTimestampHandler(zoomRW(_ => 0.0)((m, _) => m))
    )
  }
}
