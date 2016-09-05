package com.github.eerohele.expek

import java.net.URI

import org.specs2.matcher.ResultMatchers
import org.specs2.mutable

// scalastyle:off multiple.string.literals magic.number

class XsltSpecificationSpec  extends mutable.Specification with ResultMatchers with XsltSpecification {
    val stylesheet = XSLT.file(System.getProperty("specs2.stylesheet.test"))

    override val parameters = asMap(
        "int"    -> 1,
        "string" -> "parameter",
        "uri"    -> new URI("http://www.dita-ot.org")
    )

    "Applying a template" should {
        "succeed when calling with parenthesis and expecting the correct value" >> {
            applying(<foo/>) must produce(<bar/>) must beSuccessful
        }

        "succeed when calling with curly braces and expecting the correct value" in {
            applying { <foo/> } must produce { <bar/> } must beSuccessful
        }

        "fail when expecting the incorrect value" in {
            applying { <foo/> } must produce { <baz/> } must beFailing
        }

        "work when using a mode and" should {
            "succeed when expecting the correct value" in {
                applying { <foo/> } withMode("baz") must produce { <baz/> } must beSuccessful
            }

            "fail when expecting the incorrect value" in {
                applying { <foo/> } withMode("baz") must produce { <bar/> } must beFailing
            }
        }

        "work when calling with template parameters and" should {
            "succeed when expecting the correct value" in {
                applying { <quux/> } withParameters(tunnel = false, "quux" -> "quux") must produce {
                        <bar quux="quux"/>
                } must beSuccessful
            }

            "fail when expecting the incorrect value" in {
                applying { <quux/> } withParameters(tunnel = false, "quux" -> "quux") must produce {
                        <bar quux="corgle"/>
                } must beFailing
            }
        }

        "work when setting the context node" should {
            "succeed when accessing the correct node" in {
                applying(query = "ancestor/descendant") {
                    <ancestor copied="value"><descendant/></ancestor>
                } must produce(<descendant copied="value"/>) must beSuccessful
            }

            "fail when accessing the incorrect node" in {
                applying(query = "ancestor") {
                    <ancestor copied="value"><descendant/></ancestor>
                } must produce(<descendant copied="value"/>) must beFailing
            }
        }

        "that accesses a file" should {
            "must succeed when correctly supplying a mock file" in withFiles(MockFile("b.xml", <foo/>)) {
                applying(<include href="b.xml"/>) must produce(<foo/>) must beSuccessful
            }
        }

        "succeed when correctly expecting the application to produce an empty value" in {
            applying(<empty/>).result must beEmpty must beSuccessful
        }

        "fail when incorrectly expecting the application to produce an empty value" in {
            applying(<x/>).result must beEmpty must beFailing
        }
    }

    "Calling a template" should {
        "succeed when expecting the correct value" in {
            callingTemplate("sum").withParameters(tunnel = false, "a" -> 1, "b" -> 2) must produce(3) must beSuccessful
        }

        "fail when expecting the incorrect value" in {
            callingTemplate("sum").withParameters(tunnel = false, "a" -> 1, "b" -> 2) must produce(0) must beFailing
        }

        "succeed when supplying a context node that exists" in {
            callingTemplate("a-to-b", contextNode = <a x="y"/>) must produce(<b x="y"/>) must beSuccessful
        }

        "succeed when calling a template that accesses global parameters" in {
            callingTemplate("return-global-params") must produce(1, "parameter", new URI("http://www.dita-ot.org").toString) must beSuccessful
        }

        "succeed when correctly supplying global parameters" in {
            callingTemplate("return-global-params").withStylesheetParameters(
                "int" -> 1, "string" -> "parameter", "uri" -> new URI("http://www.dita-ot.org")
            ) must produce(1, "parameter", new URI("http://www.dita-ot.org").toString) must beSuccessful
        }
    }

    "Calling a function" should {
        "succeed when using implicit namespace" in {
            implicit val namespace = "local"
            callingFunction("increment")(10) must produce(11) must beSuccessful
        }

        "succeed when calling a function that returns an atomic value" in {
            callingFunction("local", "increment")(1) must produce(2) must beSuccessful
        }

        "succeed when calling a function that takes an attribute and expecting the correct result" in {
            callingFunction("local", "rename-to-baz")(attribute("foo" -> "bar")) must produce(attribute("baz" -> "quux")) must beSuccessful
        }

        "fail when calling a function that takes an attribute and expecting the incorrect result" in {
            callingFunction("local", "rename-to-baz")(attribute("foo" -> "bar")) must produce(attribute("baz" -> "corgle")) must beFailing
        }

        "succeed when calling a function that takes mixed parameters" in {
            callingFunction("local", "mix")(1, element(<foo/>), "bar") must produce("bar", element(<foo/>), 1) must beSuccessful
        }
    }

    "Filtering" should {
        "ignore an attribute that matches the given XPath expression" in {
            applying(<x/>) must produce(<y/>)(filterAttr(!XPath.matches("@id", _))) must beSuccessful
        }

        "not ignore the attribute if the XPath expression doesn't match" in {
            applying(<x/>) must produce(<y/>)(filterAttr(XPath.matches("@id", _))) must beFailing
        }
    }
}
