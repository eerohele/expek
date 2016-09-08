package com.github.eerohele.expek

import java.io.StringReader
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api.XdmNode

import scala.xml.{Node, XML}

package object utils {
    private[expek] implicit class Tap[A](private val a: A) extends AnyVal {
        def tap[B](f: A => Unit): A = { f(a); a }
    }

    private[expek] object NodeConversions {
        /** Convert [[Node]] to [[String]]. */
        implicit def nodeToString(node: Node): String = node.buildString(true)

        /** Convert [[Node]] to [[Source]]. */
        implicit def nodeToSource(node: Node): Source = new StreamSource(new StringReader(node))

        /** Convert [[XdmNode]] to [[Node]]. */
        implicit def xdmNodeToScalaXmlNode(node: XdmNode): Node = XML.loadString(node.toString)
    }
}
