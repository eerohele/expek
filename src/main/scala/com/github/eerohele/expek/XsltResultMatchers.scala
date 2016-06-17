package com.github.eerohele.expek

import java.math.BigInteger
import javax.xml.transform.Source

import net.sf.saxon.s9api.{XdmNode, XdmNodeKind, XdmValue}
import org.hamcrest.StringDescription
import org.specs2.matcher.{Expectable, MatchResult, MatchResultCombinators, Matcher}
import org.xmlunit.matchers.CompareMatcher

import scala.xml.Node

/** A specs2 matcher that matches against the result of an XSLT transformation.
  *
  * The result of an XSLT transformation can either be a node, which can be one of:
  *
  *  - Element
  *  - Document
  *  - Processing instruction
  *  - Text
  *  - Attribute
  *  - Comment
  *
  * Or it can be an atomic value, which, when converted into a native Java type, can be one of:
  *
  *  - [[String]]
  *  - [[BigInteger]]
  *  - [[BigDecimal]]
  *  - [[Double]]
  *  - [[Float]]
  *  - [[Boolean]]
  *  - [[QName]]
  *  - [[URI]]
  *
  * Element and document nodes are converted into [[Source]] so that we can compare them with XMLUnit.
  *
  * [[Node]] instances in the `expected` [[Vector]] are also converted into a [[Source]] so that we can compare
  * those to XSLT transformation results.
  *
  * Other node types are kept as is and we compare their string representations.
  *
  * Atomic values that are the result of an XSLT transformation are converted to native Java types with Saxon's
  * `XdmAtomicValue.getValue()` method and compared to the expected values.
  *
  */
sealed class XsltResultMatcher[T <: Transformation](expected: Vector[Any])(implicit matcher: Source => CompareMatcher)
    extends Matcher[T] with MatchResultCombinators {

    def apply[S <: T](expectable: Expectable[S]): MatchResult[S] = {
        expected.zip(expectable.value.result).map((intoResult[S](expectable) _).tupled).reduceLeft(_ and _)
    }

    private def intoResult[S <: T](expectable: Expectable[S])(expected: Any, actual: Any): MatchResult[S] = {
        (expected, actual) match {
            /** If [[expected]] and [[actual]] are instances of [[Source]], they are either XML element or document
              * nodes. In that case, compare them with XMLUnit.
              */
            case (e: Source, a: Source) => {
                val compareMatcher = matcher(e)

                result(
                    compareMatcher.matches(a),
                    s"$a is equal to $e",
                    createKoMessage(compareMatcher, a).toString,
                    expectable
                )
            }

            /** If [[expected]] and [[actual]] are instances of [[XdmNode]], they are one of:
              *
              * - A `text()` node
              * - A `processing-instruction()` node
              * - An `attribute()` node
              *
              * In that case, compare their string representations.
              */
            case (e: XdmNode, a: XdmNode) => {
                result(
                    e.toString == a.toString, s"$a is equal to $e", s"$a is not equal to $e", expectable
                )
            }

            /** In any other case, [[expected]] and [[actual]] are native Java types and we can compare them directly.
              */
            case (a: Any, e: Any) => result(a == e, s"$a is equal to $e", s"$a is not equal to $e", expectable)
        }
    }

    private def createKoMessage(compareMatcher: CompareMatcher, actual: Source): StringDescription = {
        val description = new StringDescription
        description.appendDescriptionOf(compareMatcher).appendText("\n     but ")
        compareMatcher.describeMismatch(actual, description)
        description
    }
}

/** A trait you can mix in to your specs2 specification to compare the results of [[Transformation]] instances to
  * a sequence of expected values.
  */
trait XsltResultMatchers {
    // scalastyle:off method.name
    /** Create a matcher that compares the supplied arguments against the result of an XML transformation.
      *
      * Example use:
      *
      * {{{
      * class MySpecification extends mutable.Specification with XsltSpecification {
      *     val stylesheet = new File("my-stylesheet.xsl")
      *
      *     "Convert a into b" in {
      *         // Apply the templates for the <a> element in the XSLT stylesheet and check the result.
      *         applying(<a>foo</a>) must <->(<b>foo</b>)
      *     }
      * }
      * }}}
      */
    def <->(any: Any*)(implicit matcher: Source => CompareMatcher): XsltResultMatcher[Transformation] = {
        new XsltResultMatcher(any.toVector.map(convert))(matcher)
    }
    // scalastyle:on method.name

    protected def convert(value: Any) = {
        value match {
            /** An xs:integer is a [[BigInteger]], so we'll convert any [[Int]] that the user expects into a
              * [[BigInteger]] so tha they can be successfully compared without the user having to write
              * `BigInteger.valueOf(n)` all over the place.
              */
            case x: Int      => BigInteger.valueOf(x)

            /** If the expected value is an instance of [[Node]], convert it to a [[Source]] so that we can compare it
              * with XMLUnit.
              */
            case x: Node     => NodeConversions.nodeToSource(x)

            /** If the expected value is an element() or a document-node(), convert it to a [[Source]] so that we can
              * compare it with XMLUnit.
              */
            case x: XdmNode if (x.getNodeKind == XdmNodeKind.ELEMENT || x.getNodeKind == XdmNodeKind.DOCUMENT) => {
                x.asSource
            }

            case x: XdmNode  => x
            case x: XdmValue => x.asInstanceOf[XdmNode].asSource
            case x: Any      => x
        }
    }
}