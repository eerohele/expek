package com.github.eerohele.expek

import org.specs2.mutable

abstract class IdentitySpec extends mutable.Specification with XsltSpecification {
    val identityTemplate =
        <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

            <xsl:template match="@* | node()">
                <xsl:copy>
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
            </xsl:template>

        </xsl:stylesheet>

    val stylesheet = XSLT.elem(identityTemplate)
}
