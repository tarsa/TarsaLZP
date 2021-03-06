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

import _infrastructure.domtest.DomNodeInfo._
import org.scalajs.dom
import org.scalajs.dom.ext.{Attributes, PimpedNodeList}
import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.Assertion
import org.scalatest.Inspectors.forEvery
import org.scalatest.MustMatchers._

import scala.collection.mutable
import scala.scalajs.js

abstract class DomNodeInfo[Repr <: DomNodeInfo[Repr]] {
  def name: String

  protected def attributes: Map[String, String]

  protected def attribute(name: String): Option[String]

  protected def attributesCount: Int

  protected def fields: Map[String, String]

  protected def field(name: String): Option[String]

  def childNodes: Seq[Repr]

  final def propFor(propertyName: NodePropertyName): Option[String] = {
    propertyName match {
      case NodeAttributeName(name) =>
        attribute(name)
      case NodeFieldName(name) =>
        field(name)
    }
  }

  final def mustHaveProps(nodeProperties: NodeProperty*): Assertion = {
    forEvery(nodeProperties) {
      case NodeAttribute(attrName, attrValue) =>
        attribute(attrName) must contain("" + attrValue)
      case NodeField(fieldName, fieldValue) =>
        field(fieldName) must contain("" + fieldValue)
    }.withClue(LazyString(s"""
        |Available attributes: $attributes
        |Available fields: $fields""".stripMargin))
  }

  final def prettyPrint(lines: mutable.ArrayBuffer[String], indentation: Int,
      printAttributes: Boolean, printFields: Boolean,
      printChildNodes: Boolean): Unit = {
    val indent = " " * indentation
    lines += s"$indent$name"
    if (printAttributes && attributes.nonEmpty) {
      lines += s"${indent}Attributes:"
      attributes.toList.sorted.foreach {
        case (attrName, attrValue) =>
          lines += s"$indent- $attrName ($attrValue)"
      }
    }
    if (printFields && fields.nonEmpty) {
      lines += s"${indent}Fields:"
      fields.toList.sorted.foreach {
        case (fieldName, fieldValue) =>
          lines += s"$indent- $fieldName ($fieldValue)"
      }
    }
    if (printChildNodes && childNodes.nonEmpty) {
      lines += s"${indent}Child nodes:"
      childNodes.foreach { node =>
        lines += ""
        node.prettyPrint(lines, indentation + 2, printAttributes, printFields,
          printChildNodes)
      }
    }
  }

  final def prettyPrintFull(): Unit = {
    val linesBuffer = mutable.ArrayBuffer[String]()
    prettyPrint(linesBuffer, 0, printAttributes = true, printFields = true,
      printChildNodes = true)
    linesBuffer.foreach(println)
  }

  override def toString: String = {
    if (childNodes.isEmpty) {
      s""""$name""""
    } else {
      childNodes.mkString(s"$name{", "; ", "}")
    }
  }
}

object DomNodeInfo {
  def lazyMutableWrapper(domNode: dom.Node): LazyMutableWrapper =
    LazyMutableWrapper(domNode)

  def snapshot(domNode: dom.Node): Snapshot =
    Snapshot(domNode)

  sealed trait NodePropertyName {
    type PropertyType <: NodeProperty

    def raw: String
    def apply(value: js.Any): PropertyType
  }

  final case class NodeAttributeName(raw: String) extends NodePropertyName {
    override type PropertyType = NodeAttribute

    override def apply(value: js.Any): NodeAttribute =
      NodeAttribute(raw, value)
  }

  final case class NodeFieldName(raw: String) extends NodePropertyName {
    override type PropertyType = NodeField

    override def apply(value: js.Any): NodeField =
      NodeField(raw, value)
  }

  sealed trait NodeProperty {
    def name: String
    def value: js.Any
  }

  final case class NodeAttribute(name: String, value: js.Any)
      extends NodeProperty

  final case class NodeField(name: String, value: js.Any) extends NodeProperty

  /** Attributes */
  object A {
    private type AN = NodeAttributeName
    private val AN = NodeAttributeName

    val max: AN = AN("max")
    val tpe: AN = AN("type")
    val value: AN = AN("value")
  }

  /** Fields */
  object F {
    private type FN = NodeFieldName
    private val FN = NodeFieldName

    val checked: FN = FN("checked")
    val disabled: FN = FN("disabled")
    val wholeText: FN = FN("wholeText")
  }

  private val disallowedFieldNames =
    Set(
      "baseURI",
      "innerHTML",
      "namespaceURI",
      "outerHTML",
      "textContent"
    )

  private val allowedFieldTypes =
    Set(
      "boolean",
      "number",
      "string"
    )

  private[domtest] def objectFields(obj: js.Object): Map[String, String] = {
    js.Object
      .properties(obj)
      .map(fieldName => fieldName -> objectField(obj, fieldName))
      .toMap
      .collect {
        case (fieldName, Some(fieldValue)) =>
          fieldName -> fieldValue
      }
  }

  private[domtest] def objectField(obj: js.Object,
      fieldName: String): Option[String] = {
    val isUpper = fieldName == fieldName.toUpperCase
    if (isUpper || disallowedFieldNames.contains(fieldName)) {
      None
    } else {
      val d = obj.asInstanceOf[js.Dynamic]
      val fieldValue = d.selectDynamic(fieldName)
      if (allowedFieldTypes.contains(js.typeOf(fieldValue))) {
        Some("" + fieldValue)
      } else {
        None
      }
    }
  }

  private[domtest] def nodeAttributes(
      domNode: dom.Node): Map[String, String] = {
    (domNode.attributes: js.UndefOr[dom.NamedNodeMap])
      .fold(Map.empty[String, String]) { attrMap =>
        attrMap.toMap.mapValues(_.nodeValue)
      }
  }

  private[domtest] def nodeAttribute(domNode: dom.Node,
      name: String): Option[String] = {
    (domNode.attributes: js.UndefOr[dom.NamedNodeMap])
      .fold(Option.empty[String]) { attributes =>
        attributes.get(name).map(_.nodeValue)
      }
  }

  private[domtest] def nodeChildNodes(domNode: dom.Node): Seq[dom.Node] = {
    (domNode.childNodes: js.UndefOr[dom.NodeList])
      .fold(Nil: Seq[dom.Node])(nodeList => nodeList)
  }
}
