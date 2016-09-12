package com.github.eerohele.expek

import org.specs2.mutable

class NodeRefinerySpec extends mutable.Specification {
    "Setting namespace must add namespace to given element" >> {
        NodeRefinery.setNameSpace("urn:foo:bar")(<foo/>) must be_==(<foo xmlns="urn:foo:bar"/>)
    }
}
