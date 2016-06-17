package com.github.eerohele.expek

import java.io.StringReader
import java.net.URI
import java.nio.file.{FileSystem, Files, Path}
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import com.google.common.jimfs.Jimfs

import scala.xml.Elem

/** Functions for operating on files in a [[Jimfs]] file system. */
object TransientFileSystem {
    import NodeConversions.nodeToString

    /** The [[Path]] of the root of this file system. */
    def getRoot(fs: FileSystem): Path = pathFromURI(fs, new URI(Jimfs.URI_SCHEME, "/", None.orNull))

    /** Resolve a URI against the root of the file system. */
    def resolveAgainstRoot(fs: FileSystem, uri: URI): URI = getRoot(fs).toUri.resolve(uri)

    /** Given a [[URI]], get the [[Path]] of a file in the virtual file system. */
    def pathFromURI(fs: FileSystem, uri: URI): Path = fs.getPath(uri.getPath)

    /** Turn [[Elem]] into a [[Source]]. Set the base URI to a URI on the transient file system. */
    def source(fs: FileSystem, elem: Elem): Source = {
        new StreamSource(new StringReader(elem), makeTransientPath(fs, elem))
    }

    /** Turn a [[URI]] that points to a file in this file system into a [[Source]]. */
    def source(fs: FileSystem, uri: URI): Source = {
        val bytes: Array[Byte] = Files.readAllBytes(pathFromURI(fs, uri))
        new StreamSource(new StringReader(new String(bytes)), uri.toString)
    }

   /** Generate a transient path for an [[Elem]] on the given file system.
    *
    * The path consists of these components:
    *
    * - The root of the transient file system
    * - The hashcode of the [[Elem]]
    * - The .xml extension.
    */
    def makeTransientPath(fs: FileSystem, elem: Elem): String = {
        getRoot(fs).toUri.resolve(Math.abs(elem.hashCode).toString).toString + ".xml"
    }

    /** Check whether the given file system has the given file. */
    def hasFile(fs: FileSystem, file: String): Boolean = Files.exists(fs.getPath(file))
}
