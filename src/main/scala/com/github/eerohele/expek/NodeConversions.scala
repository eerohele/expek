package com.github.eerohele.expek

import java.io.StringReader
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import scala.xml.Node

/** Implicit conversions for [[Node]]. */
object NodeConversions {
    /** Convert [[Node]] to [[String]]. */
    implicit def nodeToString(node: Node): String = node.buildString(true)

    /** Convert [[Node]] to [[Source]]. */
    implicit def nodeToSource(node: Node): Source = new StreamSource(new StringReader(node))
}
