<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:local="local"
  exclude-result-prefixes="local xs">

  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="int" as="xs:integer"/>
  <xsl:param name="string" as="xs:string"/>
  <xsl:param name="uri" as="xs:anyURI"/>

  <xsl:function name="local:increment" as="xs:integer">
    <xsl:param name="integer" as="xs:integer"/>
    <xsl:sequence select="$integer + 1"/>
  </xsl:function>

  <xsl:function name="local:wrap-into-foo" as="document-node()">
    <xsl:param name="el" as="element()"/>

    <xsl:variable name="returned">
      <xsl:element name="foo">
        <xsl:sequence select="$el"/>
      </xsl:element>
    </xsl:variable>

    <xsl:sequence select="$returned"/>
  </xsl:function>

  <xsl:function name="local:rename-to-baz" as="attribute(baz)">
    <xsl:param name="attr" as="attribute(foo)"/>

    <xsl:variable name="new-attr" as="attribute()">
      <xsl:attribute name="baz">quux</xsl:attribute>
    </xsl:variable>

    <xsl:sequence select="$new-attr"/>
  </xsl:function>

  <xsl:function name="local:mix" as="item()+">
    <xsl:param name="int" as="xs:integer"/>
    <xsl:param name="el" as="element()"/>
    <xsl:param name="str" as="xs:string"/>
    <xsl:sequence select="($str, $el, $int)"/>
  </xsl:function>

  <xsl:function name="local:take-and-return-text" as="text()">
    <xsl:param name="text" as="text()"/>

    <xsl:variable name="el">
      <xsl:element name="foo">bar</xsl:element>
    </xsl:variable>

    <xsl:sequence select="$el/foo/text()"/>
  </xsl:function>

  <xsl:template match="paragraph">
    <p>
      <xsl:apply-templates select="@* | node()"/>
    </p>
  </xsl:template>

  <xsl:template match="paragraph" mode="main">
    <xsl:param name="foo"/>
    <xsl:param name="baz"/>

    <p>
      <xsl:value-of select="$foo"/>, <xsl:value-of select="$baz"/>
    </p>
  </xsl:template>

  <xsl:template match="paragraph" mode="result-document">
    <!--
    <xsl:result-document href="foo.xml">
      <p>
        <xsl:apply-templates select="@* | node()"/>
      </p>
    </xsl:result-document>
    -->
  </xsl:template>

  <xsl:template match="*" mode="returns-atomic-value" as="xs:integer">
    <xsl:value-of select="sum((1, 2, 3))"/>
  </xsl:template>

  <xsl:template name="sum" as="xs:integer">
    <xsl:param name="a" as="xs:integer"/>
    <xsl:param name="b" as="xs:integer"/>

    <xsl:value-of select="$a + $b"/>
  </xsl:template>

  <xsl:template name="a-to-b">
    <b>
      <xsl:sequence select="@x"/>
    </b>
  </xsl:template>

  <xsl:template match="include">
    <xsl:sequence select="doc(resolve-uri(@href, base-uri()))"/>
  </xsl:template>

  <xsl:template match="x">
    <y id="{generate-id()}"/>
  </xsl:template>

  <xsl:template match="descendant">
    <xsl:copy>
      <xsl:sequence select="ancestor::ancestor[1]/@copied"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="return-global-params" as="item()+">
    <xsl:sequence select="($int, $string, $uri)"/>
  </xsl:template>

  <xsl:template match="empty"/>

  <xsl:template match="simpletable">
    <table>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="." mode="class"/>
    </table>
  </xsl:template>

  <xsl:template match="*" mode="class">
    <xsl:attribute name="class" select="local-name()"/>
  </xsl:template>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
