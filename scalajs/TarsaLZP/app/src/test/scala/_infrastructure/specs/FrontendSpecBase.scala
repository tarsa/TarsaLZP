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
package _infrastructure.specs

import japgolly.scalajs.react.vdom.{HtmlAttrs, HtmlTags, VdomElement}
import org.scalajs.dom

abstract class FrontendSpecBase extends SyncSpecBase {
  def dummyTag(): (String, VdomElement) = {
    import japgolly.scalajs.react.vdom.Implicits._
    val guid = java.util.UUID.randomUUID().toString
    (guid, HtmlTags.noscript(HtmlAttrs.id := guid).render)
  }

  def findChildByClassName(parent: dom.Element,
      className: String): dom.Element = {
    import dom.ext.PimpedNodeList
    val element =
      (parent.getElementsByClassName(className): Seq[dom.Node]) match {
        case Seq(theOne) =>
          theOne
        case seq =>
          val message = s"Expected exactly one element of class $className, " +
            s"but got ${seq.size} elements"
          fail(message)
      }
    assert(element.isInstanceOf[dom.Element])
    element.asInstanceOf[dom.Element]
  }
}
