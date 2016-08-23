package com.github.eerohele.expek

import java.io.File
import java.net.URI

import com.github.eerohele.expek.{TransientFileSystem => TFS}
import com.google.common.jimfs.Jimfs
import org.specs2.mutable.Specification

// scalastyle:off multiple.string.literals

class TransientFileSystemSpec extends Specification {
    isolated

    lazy val fs = Jimfs.newFileSystem

    "Getting the root path of the file system" >> {
        val uri = TFS.getRoot(fs).toUri
        uri.getPath must be_==("/") and(uri.getScheme must be_==(Jimfs.URI_SCHEME))
    }

    "Converting URI into a Path on the file system" >> {
        TFS.pathFromURI(fs, new URI("foo")).toUri.getPath must be_==("/work/foo")
    }

    "Converting an Elem into a Source" should {
        "set the base URI of the source to point to the file system" in {
            new URI(TFS.source(fs, <foo/>).getSystemId).getScheme must be_==(Jimfs.URI_SCHEME)
        }
    }

    "Creating a path for an Elem on the file system" should {
        val uri = new URI(TFS.makeTransientPath(fs, <foo/>))

        "Return a path in the root of the file system" in {
            new File(uri.getPath).getParent must be_==("/")
        }

        "Have the Jimfs URI scheme" in {
            uri.getScheme must be_==(Jimfs.URI_SCHEME)
        }

        "Have the .xml extension" in {
            uri.toString must endWith(".xml")
        }
    }

    "Checking whether a file exists" should {
        "return false if the file doesn't exist" in {
            TFS.hasFile(fs, "/foo.xml") must beFalse
        }

        "return true if the file doesn't exist" in {
            val m = MockFile("foo.xml", <bar/>)(fs)
            m.create
            TFS.hasFile(fs, "/foo.xml") must beTrue
        }
    }
}
