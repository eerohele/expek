package com.github.eerohele.expek

import java.io.File
import java.net.URL

import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver

import scala.xml.factory.XMLLoader
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Elem, Node}

/** Functions for converting an XML file into a [[Node]] with catalog resolving enabled.
  *
  * You must set the `xml.catalog.files` system property for this to work at all.
  *
  * Code 100% stolen from http://horstmann.com/unblog/2011-12-12/catalog.html.
  */
object ResolvingXMLLoader {
    private lazy val resolver = new CatalogResolver

    private lazy val loader = new XMLLoader[Elem] {
        override def adapter = new NoBindingFactoryAdapter {
            override def resolveEntity(publicId: String, systemId: String) = {
                resolver.resolveEntity(publicId, systemId)
            }
        }
    }

    def load(url: URL): Node = loader.load(url)

    /** Load the given [[File]] with catalog resolving enabled.
      *
      * Example:
      *
      * {{{
      * System.setProperty("xml.catalog.files", "/path/to/catalog.xml")
      * ResolvingXMLLoader.load(new File("foo.xml"))
      * }}}
      */
    def load(file: File): Node = load(file.toURI.toURL)
}
