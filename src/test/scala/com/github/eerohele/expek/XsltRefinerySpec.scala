package com.github.eerohele.expek

import org.specs2.matcher.ResultMatchers

import scala.xml.Node

class XsltRefineInputSpec extends IdentitySpec with ResultMatchers {
    override val refineInput: Node => Node = NodeRefinery.setNameSpace("urn:foo:bar")

    "Using the namespace input refiner" should {
        "add the namespace to the transformation input" in {
            applying { <foo/>} must produce { <foo xmlns="urn:foo:bar"/> } must beSuccessful
        }

        "not add the namespace to the transformation output" in {
            applying { <foo/>} must produce { <foo/> } must beFailing
        }
    }
}

class XsltRefineOutputSpec extends IdentitySpec with ResultMatchers {
    override val refineOutput: Node => Node = NodeRefinery.setNameSpace("urn:foo:bar")

    "Using the namespace output refiner" should {
        "add the namespace to the transformation output" in {
            applying { <foo xmlns="urn:foo:bar"/> } must produce { <foo/> } must beSuccessful
        }

        "not add the namespace to the transformation input" in {
            applying { <foo/> } must produce { <foo/> } must beFailing
        }
    }
}
