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
    "Simple transformation" >> {
        applying(<paragraph>foo</paragraph>) must produce(<p>foo</p>)
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
    "Set mode and parameters" >> {
        applying(<paragraph>foo</paragraph>)
            .withMode("main")
            .withParameters(tunnel = false, "foo" -> "bar", "baz" -> "quux") must produce(<p>bar, quux</p>)
    }

    /** Apply a template that returns an atomic value instead of an element and check the result.
      *
      * You must define the `@as` attribute on your `<xsl:template>` element for this to work.
      */
    "Apply a template that returns an atomic value" >> {
        // <-> is an alias for "produce".
        applying(<anyElement/>).withMode("returns-atomic-value") must <->(6)
    }

    /** If your template accesses a node that is an ancestor or a sibling of the current node, pass in the parent node
      * of your target node. It will be set as the context item for the transformation. You can then supply an XPath
      * query that selects the node you want to apply the templates for.
      */
    "Apply a template that accesses an ancestor node" >> {
        applying(
            <ancestor copied="value"><descendant/></ancestor>,
            XPath.select("ancestor/descendant")
        ) must produce (
            <descendant copied="value"/>
        )
    }

    /** Call a function and check the result. */
    "Call a function that returns an atomic value" >> {
        // You can either pass the namespace URI of the function explicitly...
        callingFunction("local", "increment")(1) must produce(2)
    }

    "Call a function that takes an element and returns a document node" >> {
        // ...or you can define an implicit namespace in your specification so that you don't have to.
        callingFunction("wrap-into-foo")(element(<bar/>)) must produce(<foo><bar/></foo>)
    }

    /** If your function takes or returns a node type that cannot be represented by Scala's XML literals, you can use
      * the supplied constructors to give and match those node types.
      */
    "Call a function that takes and returns an attribute" >> {
        callingFunction("rename-to-baz")(attribute("foo" -> "bar")) must produce(attribute("baz" -> "quux"))
    }

    "Call a function that takes and returns a text node" >> {
        callingFunction("take-and-return-text")(text("foo")) must produce(text("bar"))
    }

    /** Check templates and functions that take or return a sequence with mixed types. */
    "Call a function that takes mixed parameters" >> {
        callingFunction("mix")(1, element(<foo/>), "bar") must produce("bar", element(<foo/>), 1)
    }

    /** Any parametes you set by overriding the "parameters" val are available for all tests in this specification. */
    "Call a named template that accesses global parameters" >> {
        callingTemplate("return-global-params") must produce(1, "parameter", new URI("http://www.dita-ot.org").toString)
    }

    "Call a named template that returns an atomic value" >> {
        callingTemplate("sum").withParameters(tunnel = false, "a" -> 1, "b" -> 2) must produce(3)
    }

    "Call a named template with a context node" >> {
        callingTemplate("a-to-b", contextNode = <a x="y"/>) must produce(<b x="y"/>)
    }

    /** If your XSLT code calls the `doc()` or `document()` XPath functions, you can create temporary mock XML files
      * that only exist for the duration of the test. They are stored in memory, not on your actual file system.
      */
    "Apply a template that calls doc()" >> withFiles(MockFile("b.xml", <foo/>)) {
        applying(<include href="b.xml"/>) must produce(<foo/>)
    }

    /** If your XML content has randomly generated content or bits you don't care about, you can configure the test to
      * ignore certain attributes or elements. */
    "Ignore an attribute" >> {
        // Define a filter that ignores the `@id` attribute.
        val af = exclude[Attr](a => a.getName == "id")
        // Create a matcher that uses the filter you created.
        val m = (s: Source) => defaultMatcher(s).withAttributeFilter(af)
        // Pass the matcher you created as the second argument to produce()`.
        applying(<x/>) must produce(<y/>)(m)
    }

    "Ignore a node that matches an XPath expression" >> {
        val m = (s: Source) => defaultMatcher(s).withNodeFilter(
            // You can also define filters that exclude nodes from the comparison based on whether they
            // match an XPath expression.
            exclude[Node](XPath.matches("table/*", _))
        )

        applying(
            <simpletable id="foo">
                <strow>
                    <stentry>bar</stentry>
                </strow>
            </simpletable>
        ) must produce(
            // If you only want to test the transformation only for the <table> element and don't care about its
            // children in this test, for instance.
            <table class="simpletable" id="foo"/>
        )(m)
    }

    "Apply a template that produces an empty value" >> {
        // If you want to use default specs2 matchers, you can match on the result vector by calling `.result`. That
        // will return the transformation result as a [[Vector]].
        applying(<empty/>).result must beEmpty
    }

    /** Documents created with `<xsl:result-document>` are also stored in the in-memory file system. */
    "<xsl:result-document> creates a file in the transient file system" >> {
        applying(<paragraph>foo</paragraph>).withMode("result-document")
            .result must beEmpty and (TransientFileSystem.hasFile(fileSystem, "foo.xml") must beTrue)
    }.pendingUntilFixed("<https://saxonica.plan.io/issues/2771>")
}
