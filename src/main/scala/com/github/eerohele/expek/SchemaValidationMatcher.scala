package com.github.eerohele.expek

import javax.xml.transform.Source

import net.sf.saxon.s9api.{Processor, XdmNode}
import org.hamcrest.StringDescription
import org.specs2.matcher.{Expectable, MatchFailure, MatchResult, MatchResultCombinators, Matcher}
import org.xmlunit.builder.Input
import org.xmlunit.matchers.ValidationMatcher
import shapeless.syntax.typeable.typeableOps

/** A specs2 matcher that validates the result of an XSLT transformation against an XML Schema.
  *
  * To validate an element against an XML Schema, that element must have a top-level definition in the schema.
  */
sealed class SchemaValidationMatcher[T <: Transformation](schema: Input.Builder)
    extends Matcher[T] with MatchResultCombinators {

    private lazy val builder = new Processor(false).newDocumentBuilder

    def apply[S <: T](expectable: Expectable[S]): MatchResult[S] = {
        val actual = expectable.value.result
        val validationMatcher = new ValidationMatcher(schema)

        /* TODO: I have absolutely no idea why `applyTemplates()` returns a TinyElementImpl all of a sudden and why I
         * need to convert it to an XdmNode and why I need to give it to the ValidationMatcher as a String instead of
         * a Source. */

        val matchResult: Vector[MatchResult[S]] = actual.map(_.cast[XdmNode].map { node =>
            result(
                validationMatcher.matches(node.toString),
                "ok",
                createKoMessage(validationMatcher, node.asSource).toString,
                expectable
            )
        }.getOrElse(MatchFailure("ok", "The transformation doesn't produce a node that can be validated.", expectable)))

        matchResult.reduceLeft(_ and _)
    }

    private def createKoMessage(matcher: ValidationMatcher, actual: Source): StringDescription = {
        val description = new StringDescription
        matcher.describeMismatch(actual, description)
        description
    }
}
