package com.github.eerohele.expek

import java.net.URI
import java.nio.file.Files

import com.google.common.jimfs.Jimfs
import org.specs2.mutable.Specification

class TransientURIResolverSpec extends Specification {
    isolated

    lazy val fs = Jimfs.newFileSystem
    lazy val resolver = new TransientURIResolver(fs)

    "Resolving a URI with the resolver" should {
        "Return a Source on the transient file system if the given URI uses the Jimfs scheme" in {
            val file = fs.getPath("/foo.xml")
            Files.createFile(file)
            val path = file.toUri.toString
            val uri = new URI(resolver.resolve(path, "").getSystemId)
            uri.getScheme must be_==(Jimfs.URI_SCHEME) and(uri.getPath must be_==("/foo.xml"))
        }

        "Should delegate to the superclass resolver if the URI uses any other scheme" in {
            val uri = new URI(resolver.resolve("/foo.xml", "").getSystemId)
            uri.getScheme must be_==("file") and(uri.getPath must be_==("/foo.xml"))
        }
    }
}
