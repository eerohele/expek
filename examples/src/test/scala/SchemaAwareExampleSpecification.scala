package com.github.eerohele.expek
package examples

import org.specs2.mutable
import org.xmlunit.builder.Input

class SchemaAwareExampleSpecification extends mutable.Specification with ValidatingXsltSpecification with SchemaValidationMatchers {
    val stylesheet = XSLT.elem(
        <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

            <xsl:template match="foo">
                <bar>
                    <xsl:apply-templates select="@*"/>
                </bar>
            </xsl:template>

            <!-- Copy the value of the @quux attribute into the @grault attribute. -->
            <xsl:template match="@quux">
                <xsl:attribute name="grault" select="."/>
            </xsl:template>

        </xsl:stylesheet>
    )

    override val inputSchema = Some(Input.fromString(
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
            <xs:element name="foo">
                <xs:complexType>
                    <xs:attribute name="quux" type="xs:string" default="corge"/>
                </xs:complexType>
            </xs:element>

            <xs:element name="bar">
                <xs:complexType>
                    <xs:attribute name="grault" type="xs:string"/>
                </xs:complexType>
            </xs:element>
        </xs:schema>.toString
    ))

    implicit val outputSchema = inputSchema.get

    "Validate and load default attributes from input XML and validate output XML" >> {
        applying { <foo/> } must produce { <bar grault="corge"/> }
    }
}
