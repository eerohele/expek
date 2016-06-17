package com.github.eerohele.expek

import java.net.URI
import java.nio.file.FileSystem
import javax.xml.transform.Source

import com.google.common.jimfs.Jimfs
import org.apache.xml.resolver.tools.CatalogResolver

/** A [[CatalogResolver]] that resolves any [[URI]] that uses the [[Jimfs.URI_SCHEME]] scheme against a
  * [[Jimfs]] filesystem.
  *
  * For any other type of [[URI]], it delegates to [[CatalogResolver]].
  */
class TransientURIResolver(fileSystem: FileSystem) extends CatalogResolver {
    override def resolve(href: String, base: String): Source = {
        val uri: URI = new URI(href)

        if (uri.getScheme == Jimfs.URI_SCHEME) {
            TransientFileSystem.source(fileSystem, uri)
        } else {
            super.resolve(href, base)
        }
    }
}
