package com.github.eerohele.expek

import org.specs2.mutable
import org.specs2.matcher.ResultMatchers
import org.xmlunit.builder.Input

class ValidatingXsltSpecificationSpec extends mutable.Specification with ResultMatchers with ValidatingXsltSpecification {
    val identityTemplate =
        <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

            <xsl:template match="@* | node()">
                <xsl:copy>
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
            </xsl:template>

        </xsl:stylesheet>

    val stylesheet = XSLT.elem(identityTemplate)

    implicit val outputSchema = Input.fromString(
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
            <xs:element name="foo" type="xs:string"/>
        </xs:schema>.toString
    )

    "Must succeed is XML is valid according to implicit schema" >> {
        applying { <foo/> } must produce { <foo/> } must beSuccessful
    }

    "Must fail is XML is invalid according to implicit schema" >> {
        applying { <bar/> } must produce { <bar/> } must beFailing
    }
}
