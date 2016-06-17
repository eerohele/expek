package com.github.eerohele.expek

import java.net.URI

import net.sf.saxon.s9api.{QName, XdmValue}
import org.specs2._

class Any2XdmSpec extends mutable.Specification {
    "asMap()" should {
        "turn a sequence of tuples into a XSLT parameter map" in {
            Any2Xdm.asMap(
              "string" -> "a", "int" -> 1, "uri" -> new URI("http://github.com")
            ) must haveSuperclass[Map[QName, XdmValue]].and(haveSize(3))
        }
    }
}
