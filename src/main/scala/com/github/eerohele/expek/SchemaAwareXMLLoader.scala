package com.github.eerohele.expek

import javax.xml.XMLConstants
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import javax.xml.validation.{Schema, SchemaFactory}

import org.xml.sax.InputSource
import org.xmlunit.builder.Input

import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Node, Source, TopScope}

/* A factory adapter that validates the loaded XML against an XML schema.
 *
 * Code stolen from https://github.com/EdgeCaseBerg/scala-xsd-validation/blob/6c3f428cf17be0a303a9fd46d5dc4fb3c0bb463b/src/main/scala/SchemaAwareFactoryAdapter.scala.
 */
sealed class SchemaAwareFactoryAdapter(schema: Schema) extends NoBindingFactoryAdapter {
    def loadXML(source: InputSource): Node = {
        val parser: SAXParser = try {
            val f = SAXParserFactory.newInstance
            f.setNamespaceAware(true)
            f.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
            f.newSAXParser
        } catch {
            case e: Exception => throw e
        }

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

object SchemaAwareXMLLoader {
    def apply(input: Node, schema: Input.Builder): Node = {
        val sf: SchemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val s: Schema = sf.newSchema(schema.build)
        new SchemaAwareFactoryAdapter(s).loadXML(Source.fromString(input.toString))
    }
}
