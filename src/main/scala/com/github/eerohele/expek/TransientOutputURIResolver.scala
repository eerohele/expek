package com.github.eerohele.expek

import java.net.URI
import java.nio.file.{FileSystem, Files}
import javax.xml.transform.Result
import javax.xml.transform.stream.StreamResult

import net.sf.saxon.lib.StandardOutputResolver

/** A [[StandardOutputResolver]] that resolves all URIs to point to a new file on the supplied [[Jimfs]] file system.
  *
  * If you configure Saxon to use this URI resolver, any document created via `<xsl:result-document>` will be saved on
  * the given transient file system.
  */
class TransientOutputURIResolver(fileSystem: FileSystem) extends StandardOutputResolver {
    override def resolve(href: String, base: String): Result = {
        new StreamResult(Files.newOutputStream(TransientFileSystem.pathFromURI(fileSystem, new URI(href))))
    }
}
