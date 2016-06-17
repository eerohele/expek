package com.github.eerohele.expek

import java.net.URI
import java.nio.file.{FileSystem, Files, Path}

import scala.xml.Elem

/** A mock file that lives in a [[Jimfs]] file system.
  *
  * @param uri The [[URI]] of the mock file.
  * @param elem The XML ([[Elem]]) content of the file.
  */
case class MockFile(uri: URI, elem: Elem)(fileSystem: FileSystem) {
    import NodeConversions._

    val path: Path = TransientFileSystem.pathFromURI(fileSystem, resolveAgainstRoot(uri))

    private def resolveAgainstRoot(uri: URI): URI = TransientFileSystem.getRoot(fileSystem).toUri.resolve(uri)

    def create(): Unit = {
        Files.createDirectories(path.getParent)
        Files.createFile(path)
        Files.write(path, elem.getBytes)
    }

    def delete(): Unit = Files.delete(path)

    override def toString = uri.toString
}

object MockFile {
    /** A [[MockFile]] constructor that takes a [[String]]. */
    def apply(uri: String, elem: Elem): FileSystem => MockFile = MockFile(new URI(uri), elem)(_)
}
