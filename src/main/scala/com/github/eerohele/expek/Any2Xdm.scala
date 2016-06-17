package com.github.eerohele.expek

import java.io.StringReader
import java.math.BigDecimal
import java.net.URI
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api.{Processor, QName, XdmAtomicValue, XdmItem, XdmNode}

import scala.xml.Elem

/** Methods for converting Java values into XML data model (XDM) values. */
object Any2Xdm {
    import NodeConversions.nodeToString

    private lazy val builder = new Processor(false).newDocumentBuilder

    /** Convert a Java value into an [[XdmItem]].
      *
      * Supports any value that has an [[XdmAtomicValue]] [[http://www.saxonica.com/documentation9.5/javadoc/net/sf/expek/s9api/XdmAtomicValue.html constructor]],
      * as well as [[Elem]], which is converted into an [[XdmNode]].
      */
    def into(any: Any): XdmItem = {
        any match {
            case x: XdmItem    => x
            case x: Elem       => builder.build(new StreamSource(new StringReader(x)))
            case x: Int        => new XdmAtomicValue(x.toLong)

            // FIXME: There must be a better way of doing this.
            case x: BigDecimal => new XdmAtomicValue(x)
            case x: Boolean    => new XdmAtomicValue(x)
            case x: Double     => new XdmAtomicValue(x)
            case x: Float      => new XdmAtomicValue(x)
            case x: Long       => new XdmAtomicValue(x)
            case x: QName      => new XdmAtomicValue(x)
            case x: String     => new XdmAtomicValue(x)
            case x: URI        => new XdmAtomicValue(x)

            // FIXME: Don't thrown an exception.
            case _ => throw new Exception(s"Can't convert $any into an XdmItem.")
        }
    }

    /** Convert any number of `(String, Any)` tuples into an XSLT parameter [[Map]]. */
    def asMap(tuple: (String, Any)*): Map[QName, XdmItem] = tuple.foldLeft(Map[QName, XdmItem]()) {
        case (acc, (k, v)) => acc + (new QName(k) -> into(v))
    }
}
