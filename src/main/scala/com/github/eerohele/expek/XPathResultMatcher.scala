package com.github.eerohele.expek

import net.sf.saxon.s9api.XdmNode
import org.specs2.matcher.{Expectable, MatchFailure, MatchResult, MatchResultCombinators, Matcher}

import shapeless.syntax.typeable.typeableOps

/** A specs2 matcher that validates the result of an XSLT transformation against an XPath expression.
  *
  * TODO: There's duplicate code between this and [[SchemaValidationMatcher]].
  */
class XPathResultMatcher[T <: Transformation](matcher: (String, XdmNode) => Boolean)(query: String)
    extends Matcher[T] with MatchResultCombinators {

    def apply[S <: T](expectable: Expectable[S]): MatchResult[S] = {
        val actual = expectable.value.result

        val matchResult: Vector[MatchResult[S]] = actual.map(_.cast[XdmNode].map { node =>
            result(
                matcher(query, node),
                "ok",
                s"""${node.toString} doesn't match XPath expression "${query}"""",
                expectable
            )
        }.getOrElse(MatchFailure("ok", "The transformation doesn't produce a node that can be validated.", expectable)))

        matchResult.reduceLeft(_ and _)
    }
}
