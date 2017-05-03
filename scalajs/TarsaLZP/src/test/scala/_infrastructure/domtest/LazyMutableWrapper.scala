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
package _infrastructure.domtest

import org.scalajs.dom

class LazyMutableWrapper private (domNode: dom.Node)
    extends DomNodeInfo[LazyMutableWrapper] {
  override def name: String =
    domNode.nodeName

  protected lazy val attributes: Map[String, String] =
    DomNodeInfo.nodeAttributes(domNode)

  protected override def attribute(name: String): Option[String] =
    DomNodeInfo.nodeAttribute(domNode, name)

  override protected def attributesCount: Int =
    attributes.size

  protected lazy val fields: Map[String, String] =
    DomNodeInfo.objectFields(domNode)

  protected override def field(name: String): Option[String] =
    DomNodeInfo.objectField(domNode, name)

  lazy val childNodes: Seq[LazyMutableWrapper] =
    DomNodeInfo.nodeChildNodes(domNode).map(LazyMutableWrapper(_))
}

object LazyMutableWrapper {
  def apply(domNode: dom.Node): LazyMutableWrapper =
    new LazyMutableWrapper(domNode)
}
