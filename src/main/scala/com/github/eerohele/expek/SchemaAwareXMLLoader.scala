package com.github.eerohele.expek

import javax.xml.XMLConstants
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import javax.xml.validation.{Schema, SchemaFactory}

import org.xml.sax.InputSource
import org.xmlunit.builder.Input

import scala.util.Try
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Node, Source, TopScope}

/* A factory adapter that validates the loaded XML against an XML schema.
 *
 * Code stolen from https://github.com/EdgeCaseBerg/scala-xsd-validation/blob/6c3f428cf17be0a303a9fd46d5dc4fb3c0bb463b/src/main/scala/SchemaAwareFactoryAdapter.scala.
 */
sealed class SchemaAwareFactoryAdapter(schema: Schema) extends NoBindingFactoryAdapter {
    private def getSAXParser: Try[SAXParser] = Try {
        val f = SAXParserFactory.newInstance
        f.setNamespaceAware(true)
        f.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
        f.newSAXParser
    }

    def loadXML(source: InputSource): Try[Node] = {
        getSAXParser.map { parser =>
            val xr = parser.getXMLReader
            val vh = schema.newValidatorHandler
            vh.setContentHandler(this)
            xr.setContentHandler(vh)

            scopeStack.push(TopScope)
            xr.parse(source)
            scopeStack.pop

            rootElem
        }
    }
}

/** An XML loader that takes a [[Node]] and an XML Schema and returns the same node after validating it and loading any
  * possible default attributes that the schema defines for the input node.
  *
  * Example:
  *
  * {{{
  * SchemaAwareXMLLoader(<ph> ... </ph>, Input.fromFile("/path/to/dita/topic.xsd"))
  * // The <ph> element with the default DITA @class attribute value loaded from the given schema.
  * => <ph class="- topic/ph "> ... </ph>
  * }}}
  */
object SchemaAwareXMLLoader {
    def apply(input: Node, schema: Input.Builder): Try[Node] = {
        val sf: SchemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val s: Schema = sf.newSchema(schema.build)
        new SchemaAwareFactoryAdapter(s).loadXML(Source.fromString(input.toString))
    }
}
