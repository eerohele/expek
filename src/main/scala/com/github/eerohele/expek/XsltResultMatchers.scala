package com.github.eerohele.expek

import java.math.BigInteger
import javax.xml.transform.Source

import net.sf.saxon.s9api.{XdmNode, XdmNodeKind, XdmValue}
import org.hamcrest.StringDescription
import org.specs2.matcher.{AnyMatchers, Expectable, MatchFailure, MatchResult, MatchResultCombinators, Matcher}
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
        val actual: Vector[Any] = expectable.value.result

        val result = expected.zip(actual).map((intoResult[S](expectable) _).tupled)

        if (result.nonEmpty) {
            result.reduceLeft(_ and _)
        } else {
            MatchFailure("ok", "The transformation produces an empty value.", expectable)
        }
    }

    private def intoResult[S <: T](expectable: Expectable[S])(expected: Any, actual: Any): MatchResult[S] = {
        (expected, actual) match {
            /** If [[expected]] is a [[Source]] and [[actual]] is an [[XdmNode]], they are either XML element or document
              * nodes. In that case, compare them with XMLUnit.
              */
            case (e: Source, a: XdmNode) => {
                val compareMatcher = matcher(e)

                result(
                    compareMatcher.matches(a.asSource),
                    "ok",
                    createKoMessage(compareMatcher, a.asSource).toString,
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
                    e.toString == a.toString, "ok", s"$a is not equal to $e", expectable
                )
            }

            /** In any other case, [[expected]] and [[actual]] are native Java types and we can compare them directly.
              */
            case (e: Any, a: Any) => result(
                e == a,
                "ok", s"$a (${a.getClass.getName}) is not equal to $e (${e.getClass.getName})",
                expectable
            )
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
trait XsltResultMatchers extends AnyMatchers {
    import utils.NodeConversions.nodeToSource

    /** A function that transforms your expected XML before it's compared with the actual XML.
      *
      * NOOP by default. See [[NodeRefinery]] for examples on the kinds of functions you might want to use this with.
      */
    def refineOutput: Node => Node = identity

    /** Create a matcher that compares the supplied arguments against the result of an XML transformation.
      *
      * Example use:
      *
      * {{{
      * class MySpecification extends mutable.Specification with XsltSpecification {
      *     val stylesheet = XSLT.file("my-stylesheet.xsl")
      *
      *     "Convert a into b" in {
      *         // Apply the templates for the <a> element in the XSLT stylesheet and check the result.
      *         applying { <a>foo</a> } must produce { <b>foo</b> }
      *     }
      * }
      * }}}
      */
    def produce(result: => Vector[Any])(implicit matcher: Source => CompareMatcher): Matcher[Transformation] = {
        new XsltResultMatcher(result.map(convert))(matcher)
    }

    def produce(any: Any*)(implicit matcher: Source => CompareMatcher): Matcher[Transformation] = {
        produce(any.toVector)(matcher)
    }

    /** Create a matcher that checks whether your transformation produces nothing.
      *
      * Example:
      *
      * {{{
      * <!-- stylesheet.xsl -->
      * <xsl:template match="a"/>
      *
      * // MySpecification.scala
      * applying { <a/> } producesNothing
      * }}}
      */
    def produceNothing[T <: Transformation]: Matcher[T] = new Matcher[T] {
        def apply[S <: T](iterable: Expectable[S]): MatchResult[S] = {
            result(iterable.value.result.isEmpty,
                   iterable.description + " produces nothing",
                   iterable.description + " produces something", iterable)
        }
    }

    // scalastyle:on method.name

    protected def convert(value: Any) = {
        value match {
            /** An xs:integer is a [[BigInteger]], so we'll convert any [[Int]] that the user expects into a
              * [[BigInteger]] so that they can be successfully compared without the user having to write
              * `BigInteger.valueOf(n)` all over the place.
              */
            case x: Int      => BigInteger.valueOf(x)

            /** If the expected value is an instance of [[Node]], convert it to a [[Source]] so that we can compare it
              * with XMLUnit.
              */
            case x: Node     => (refineOutput andThen nodeToSource)(x)

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
