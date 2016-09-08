package com.github.eerohele.expek

import java.io.{File, StringReader}
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api._

import scala.xml.Elem

trait XsltSupport {
    import utils.NodeConversions._

    val xsltCompiler: XsltCompiler

    /** Functions for converting an XSLT stylesheet into a [[Source]]. */
    object XSLT {
        /** Read a stylesheet from a file. */
        def file(xslt: String): Source = file(new File(xslt))

        /** Read a stylesheet from a file. */
        def file(xslt: File): Source = new StreamSource(xslt)

        /** Read a stylesheet from an [[Elem]]. */
        def elem(elem: Elem): Source = new StreamSource(new StringReader(elem))

        /** Transform an [[Elem]] with the given stylesheet. */
        def transform[T <: XdmValue](stylesheet: Source, elem: Elem): T = {
            val t: Xslt30Transformer = xsltCompiler.compile(stylesheet).load30
            t.applyTemplates(elem).asInstanceOf[T]
        }
    }
}

