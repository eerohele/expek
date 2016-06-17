package com.github.eerohele.expek

import java.net.URI

import org.specs2.mutable.Specification
import com.github.eerohele.expek.{TransientFileSystem => TFS}
import com.google.common.jimfs.Jimfs

class TransientFileSystemSpec extends Specification {
    lazy val fs = Jimfs.newFileSystem

    "Getting the root path of the file system" should {
        "return the root Path of the given file system" in {
            val uri = TFS.getRoot(fs).toUri
            uri.getPath must be_==("/") and(uri.getScheme must be_==(Jimfs.URI_SCHEME))
        }
    }

    "Turning URI into a Path" should {
        "work" in {
            TFS.pathFromURI(fs, new URI("foo")).toUri.getPath must be_==("/work/foo")
        }
    }
}
