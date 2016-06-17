package com.github.eerohele.expek
package examples

import java.net.URI
import javax.xml.transform.Source

import org.specs2._
import org.w3c.dom.Attr

// scalastyle:off multiple.string.literals magic.number

/** Use any [[https://etorreborre.github.io/specs2/guide/SPECS2-3.8.3/org.specs2.guide.Structure.html#styles
  * specification style that specs2 supports]].
  */
class ExampleSpecification extends mutable.Specification with XsltSpecification {
    /** The stylesheet you're testing. */
    val stylesheet = XSLT.file(System.getProperty("specs2.stylesheet.test"))

    /** The global stylesheet parameters. You define template-specific parameters on a per-test basis. */
    override val parameters = asMap(
        "int"    -> 1,
        "string" -> "parameter",
        "uri"    -> new URI("http://www.dita-ot.org")
    )

    /** The namespace that the functions defined in the stylesheets use. Note the `implicit` keyword. */
    implicit def namespace: String = "local"

    /** A simple specification.
      *
      * This is equivalent to calling <xsl:apply-templates/> on the input <paragraph> element and checking that the
      * output is a <p> element with the same content.
      */
    "Simple transformation" in {
        applying(<paragraph>foo</paragraph>) must <->(<p>foo</p>)
    }

    /* Apply a template with the given mode and parameters and check the result.
     *
     * Equivalent to:
     *
     * {{{
     * <xsl:apply-templates mode="main">
     *   <xsl:with-param name="foo" tunnel="no">bar</xsl:with-param>
     *   <xsl:with-param name="baz" tunnel="no">quux</xsl:with-param>
     * </xsl:apply-templates>
     * }}}
     */
    "Set mode and parameters" in {
        applying(<paragraph>foo</paragraph>)
            .withMode("main")
            .withParameters(tunnel = false, "foo" -> "bar", "baz" -> "quux") must <->(<p>bar, quux</p>)
    }

    /** Apply a template that returns an atomic value instead of an element and check the result.
      *
      * You must define the `@as` attribute on your `<xsl:template>` element for this to work.
      */
    "Apply a template that returns an atomic value" in {
        applying(<anyElement/>).withMode("returns-atomic-value") must <->(6)
    }

    "Apply a template that accesses an ancestor node" in {
        applying(
            <ancestor copied="value"><descendant/></ancestor>,
            XPath.select("ancestor/descendant")
        ) must <-> (
            <descendant copied="value"/>
        )
    }

    /** Call a function and check the result. */
    "Call a function that returns an atomic value" in {
        // You can either pass the namespace URI of the function explicitly...
        callingFunction("local", "increment")(1) must <->(2)
    }

    "Call a function that takes an element and returns a document node" in {
        // ...or you can define an implicit namespace in your specification so that you don't have to.
        callingFunction("wrap-into-foo")(element(<bar/>)) must <->(<foo><bar/></foo>)
    }

    /** If your function takes or returns a node type that cannot be represented by Scala's XML literals, you can use
      * the supplied constructors to give and match those node types.
      */
    "Call a function that takes and returns an attribute" in {
        callingFunction("rename-to-baz")(attribute("foo" -> "bar")) must <->(attribute("baz" -> "quux"))
    }

    "Call a function that takes and returns a text node" in {
        callingFunction("take-and-return-text")(text("foo")) must <->(text("bar"))
    }

    /** Check templates and functions that take or return a sequence with mixed types. */
    "Call a function that takes mixed parameters" in {
        callingFunction("mix")(1, element(<foo/>), "bar") must <->("bar", element(<foo/>), 1)
    }

    "Call a named template that accesses global parameters" in {
        callingTemplate("return-global-params") must <->(1, "parameter", new URI("http://www.dita-ot.org").toString)
    }

    "Call a named template that returns an atomic value" in {
        callingTemplate("sum").withParameters(tunnel = false, "a" -> 1, "b" -> 2) must <->(3)
    }

    "Call a named template and set a context node" in {
        callingTemplate("a-to-b", contextNode = <a x="y"/>) must <->(<b x="y"/>)
    }

    /** If your XSLT code calls the `doc()` or `document()` XPath functions, you can create temporary mock XML files
      * that only exist for the duration of the test. They are stored in memory, not on your actual file system.
      */
    "Apply a template that calls doc()" in withFiles(MockFile("b.xml", <foo/>)) {
        applying(<include href="b.xml"/>) must <->(<foo/>)
    }

    /** If your XML content has randomly generated content or bits you don't care about, you can configure the test to
      * ignore certain attributes or elements. */
    "Ignore an attribute" in {
        // Define a filter that ignores the `@id` attribute.
        val af = filter[Attr](a => a.getName != "id")
        val m = (s: Source) => defaultMatcher(s).withAttributeFilter(af)
        applying(<x/>) must <->(<y/>)(m)
    }

    "<xsl:result-document> creates a file in the transient file system" in {
        applying(<paragraph>foo</paragraph>).withMode("result-document")
            .result must beEmpty and (TransientFileSystem.hasFile(fileSystem, "foo.xml") must beTrue)
    }.pendingUntilFixed("<https://saxonica.plan.io/issues/2771>")
}
