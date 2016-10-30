/*
 * ## MIT License
 *
 * Copyright (c) 2015, Otto Chrons (otto@chrons.me)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package pl.tarsa.tarsalzp.ui.util

import diode._
import org.scalajs.dom._

// marker trait to identify actions that should be RAF batched
trait RAFAction extends Action

private[util] final case class RAFWrapper(action: Any, dispatch: Dispatcher)
  extends Action

final case class RAFTimeStamp(time: Double) extends Action

class RAFBatcher[M <: AnyRef] extends ActionProcessor[M] {
  private var batch = List.empty[RAFWrapper]
  private var frameRequested = false

  /**
    * Callback for RAF.
    *
    * @param time Precise time of the frame
    */
  private def nextAnimationFrame(time: Double): Unit = {
    frameRequested = false
    if (batch.nonEmpty) {
      val curBatch = batch
      batch = Nil
      // dispatch all actions in the batch
      // supports multiple different dispatchers
      curBatch.reverse.groupBy(_.dispatch).foreach {
        case (dispatch, actions) =>
          // Precede actions with a time stamp action to get correct time in
          // animations.
          // When dispatching a sequence, Circuit optimizes processing
          // internally and only calls listeners after all the actions are
          // processed
          dispatch(RAFTimeStamp(time) +: ActionBatch(actions: _*))
      }
      // request next frame
      requestAnimationFrame()
    } else {
      // got no actions to dispatch, no need to request next frame
    }
  }

  /**
    * Requests an animation frame from the browser
    * unless one has already been requested
    */
  private def requestAnimationFrame(): Unit = {
    if (!frameRequested) {
      frameRequested = true
      window.requestAnimationFrame(nextAnimationFrame _)
    }
  }

  override def process(dispatch: Dispatcher, action: Any,
    next: Any => ActionResult[M], currentModel: M) = {
    action match {
      case rafAction: RAFAction =>
        // save action into the batch using a wrapper
        batch = RAFWrapper(rafAction, dispatch) :: batch
        // request animation frame to run the batch
        requestAnimationFrame()
        // skip processing of the action for now
        ActionResult.NoChange
      case RAFWrapper(rafAction, _) =>
        // unwrap the RAF action and continue processing normally
        next(rafAction)
      case _ =>
        // default is to just call the next processor
        next(action)
    }
  }
}
