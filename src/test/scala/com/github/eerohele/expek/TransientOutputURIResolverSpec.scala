package com.github.eerohele.expek

import java.io.{ByteArrayOutputStream, StringReader, StringWriter}
import java.net.URI
import java.nio.file.Files
import javax.xml.transform.{Transformer, TransformerFactory}

import com.google.common.jimfs.Jimfs
import org.specs2.mutable.Specification

import scala.xml.XML

class TransientOutputURIResolverSpec extends Specification {
    "Resolving a URI with the resolver" should {
        "resolve the URI against the supplied file system" in {
            val fs = Jimfs.newFileSystem
            val result = new TransientOutputURIResolver(fs).resolve("/foo.xml", "")
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.transform(NodeConversions.nodeToSource(<foo/>), result)
            val str = new String(Files.readAllBytes(fs.getPath("/foo.xml")))
            XML.loadString(str) must be_==(<foo/>)
        }
    }
}
