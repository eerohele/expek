package com.github.eerohele.expek

import net.sf.saxon.s9api._
import org.w3c.dom.{Node => DomNode}

/** An object that contains XPath-related methods. */
trait XPathSupport {
    import utils.Tap

    val xpathCompiler: XPathCompiler

    val builder: DocumentBuilder

    object XPath {
        /** Evaluate the given XPath query on the given context item.
          *
          * Note: this method isn't suitable for dealing with atomic values, since it's set to always return a node.
          *
          * The primary use case is to test XSLT templates that access nodes outside the current node. This is subject
          * to change.
          *
          * Example:
          *
          * {{{
          * "Apply a template that accesses an ancestor node" in {
          *     applying {
          *         // The ancestor element is set as the context node for the transformation.
          *         <ancestor copied="value"><descendant/></ancestor>,
          *         // Use XPath to select the element that you want to apply the templates for.
          *         XPath.select("ancestor/descendant")
          *     } must produce (<descendant copied="value"/>)
          * }
          * }}}
          */
        def select(query: String)(contextItem: XdmItem): XdmNode = {
            xpathCompiler.compile(query).load.tap(_.setContextItem(contextItem)).evaluate.asInstanceOf[XdmNode]
        }

        /** Check whether an item matches an XPath expression.
          *
          * Use to filter nodes when comparing XML trees.
          *
          * Example:
          *
          * {{{
          * "Ignore an attribute" >> {
          *     applying(<x/>) must produce(<y/>)(filterAttr(!XPath.matches("@id", _)))
          * }
          * }}}
          */
        def matches(query: String, contextItem: XdmItem): Boolean = {
            val selector = xpathCompiler.compilePattern(query).load
            selector.setContextItem(contextItem)
            selector.effectiveBooleanValue
        }

        def matches(query: String, contextItem: DomNode): Boolean = {
            matches(query, builder.wrap(contextItem))
        }
    }
}

