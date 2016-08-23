package com.github.eerohele.expek

import java.nio.file.FileSystem

import com.google.common.jimfs.Jimfs
import org.specs2.mutable.Specification

class MockFileSpec extends Specification {
    isolated

    lazy val fs: FileSystem = Jimfs.newFileSystem

    "Instantiating a MockFile" should {
        "not create the mock file" in {
            MockFile("foo.xml", <bar/>)(fs)
            TransientFileSystem.hasFile(fs, "/foo.xml") must beFalse
        }

        "should resolve the path against the file system root" in {
            MockFile("foo.xml", <bar/>)(fs).path.toString must be_==("/foo.xml")
        }
    }

    "Creating a MockFile" should {
        "create it on the given file system" in {
            MockFile("foo.xml", <bar/>)(fs).create
            TransientFileSystem.hasFile(fs, "/foo.xml") must beTrue
        }
    }

    "Deleting a MockFile" should {
        "remove the MockFile from the file system" in {
            val m = MockFile("foo.xml", <bar/>)(fs)
            m.create
            m.delete
            TransientFileSystem.hasFile(fs, "/foo.xml") must beFalse
        }
    }
}
