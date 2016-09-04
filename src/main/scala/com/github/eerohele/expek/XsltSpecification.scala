package com.github.eerohele.expek

import java.io.{File, StringReader}
import java.nio.file.FileSystem
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import com.google.common.jimfs.{Jimfs, Configuration => JimfsConfiguration}
import net.sf.saxon.Configuration
import net.sf.saxon.s9api._
import org.apache.xml.resolver.tools.ResolvingXMLReader
import org.specs2.execute.{Failure, FailureException}
import org.specs2.mutable.BeforeAfter
import org.w3c.dom.{Attr, Node => DomNode}
import org.xmlunit.builder.Input
import org.xmlunit.matchers.CompareMatcher
import org.xmlunit.matchers.CompareMatcher.isSimilarTo
import org.xmlunit.util.Predicate

import scala.collection.JavaConversions.{asScalaIterator, mapAsJavaMap}
import scala.util.{Failure => TryFailure, Success => TrySuccess}
import scala.xml.{Elem, Node, ProcInstr}

private[expek] sealed abstract class Parametrized(transformer: Xslt30Transformer) {
    def withParameters(tunnel: Boolean, parameters: (String, Any)*): this.type = {
        withParameters(tunnel, Any2Xdm.asMap(parameters:_*))
    }

    def withParameters(tunnel: Boolean, parameters: Map[QName, XdmValue]): this.type = {
        transformer.setInitialTemplateParameters(parameters, tunnel)
        this
    }

    def withStylesheetParameters(parameters: (String, Any)*): this.type = {
        withStylesheetParameters(Any2Xdm.asMap(parameters:_*))
    }

    def withStylesheetParameters(parameters: Map[QName, XdmValue]): this.type = {
        transformer.setStylesheetParameters(parameters)
        this
    }
}

private[expek] sealed class TemplateApplication(transformer: Xslt30Transformer, input: XdmNode)
    extends Parametrized(transformer) with Transformation {

    def withMode(mode: String): this.type = { transformer.setInitialMode(new QName(mode)); this }

    def transformation: () => XdmValue = () => transformer.applyTemplates(input)
}

private[expek] sealed class TemplateCall(transformer: Xslt30Transformer, name: QName)
    extends Parametrized(transformer) with Transformation {

    def transformation: () => XdmValue = () => transformer.callTemplate(name)
}

private[expek] sealed class FunctionCall(transformer: Xslt30Transformer, name: QName, parameters: Array[XdmValue])
    extends Transformation {

    def transformation: () => XdmValue = () => transformer.callFunction(name, parameters)
}

/** A specification for testing an [[https://www.w3.org/TR/xslt-30/ XSLT]] stylesheet.
  *
  * Classes that mix in this trait must define [[stylesheet]], which is a [[File]] instance that points to the
  * stylesheet that you want to test.
  */

trait XsltSpecification extends XsltResultMatchers with SchemaValidationMatchers {
    import NodeConversions._
    import utils.Tap

    /** The stylesheet you want to test. */
    val stylesheet: Source

    /** Functions for converting an XSLT stylesheet into a [[Source]]. */
    object XSLT {
        /** Read a stylesheet from a file. */
        def file(xslt: String): Source = file(new File(xslt))

        /** Read a stylesheet from a file. */
        def file(xslt: File): Source = new StreamSource(xslt)

        /** Read a stylesheet from an [[Elem]]. */
        def elem(elem: Elem): Source = new StreamSource(new StringReader(elem))

        /** Transform an [[Elem]] with the given stylesheet. */
        def transform[T <: XdmValue](stylesheet: Source, elem: Elem): T = {
            val t: Xslt30Transformer = Saxon.compiler.compile(stylesheet).load30
            t.applyTemplates(elem).asInstanceOf[T]
        }
    }

    /** The XML Schema to use for validating the input XML and loading default attributes
      *
      * Example:
      *
      * {{{
      * import org.xmlunit.builder.Input
      *
      * override val inputSchema = Some(Input.fromFile("schema.xsd"))
      * }}}
      */
    val inputSchema: Option[Input.Builder] = None

    /** The default matcher for comparing two XML element or document nodes. */
    val defaultMatcher: Source => CompareMatcher = isSimilarTo(_).normalizeWhitespace

    /** A function that takes a [[Source]] and returns a [[CompareMatcher]] that compares the given
      * [[Source]] to an XML fragment.
      *
      * You can extend the default matcher by by overriding it in your specification:
      *
      * {{{
      * class MySpecification extends mutable.Specification with XsltSpecification {
      *     ...
      *
      *     // A matcher that ignores @id attributes when comparing elements.
      *     override implicit def matcher = (s: Source) => defaultMatcher(s).withAttributeFilter(
      *         filter[Attr](a => a.getName != "id")
      *     )
      *
      *     ...
      * }
      * }}}
      *
      * You can also use a different matcher for a single test:
      *
      * {{{
      * "Ignore an attribute" in {
      *     // Define a custom matcher for this test only.
      *     val m = (s: Source) => defaultMatcher(s).withAttributeFilter(
          *     // Ignore any attribute called "ignore-me".
      *         filter[Attr](a => a.getName != "ignore-me")
      *     )
      *
      *     // Pass in the custom matcher.
      *     applying(<x/>) must produce(<y/>)(m)
      * }
      * }}}
      */
    implicit def matcher: Source => CompareMatcher = defaultMatcher

    /** Global stylesheet parameters.
      *
      * You can either supply a `Map[QName, XdmValue]` or call [[asMap]] on a sequence of tuples:
      *
      * {{{
      * scala> asMap("string" -> "foo", "int" -> 1, "uri" -> new URI("http://www.google.com"))
      * res2: Map[QName, XdmItem] = Map(string -> foo, int -> 1, uri -> http://www.google.com)
      * }}}
       */
    val parameters: Map[QName, XdmValue] = Map[QName, XdmValue]()

    /** An extensible Saxon [[Configuration]] instance.
      *
      * Overriding is prevented to make it more difficult to accidentally lose the support for reading from and writing
      * to an in-memory [[Jimfs]] file system.*/
    final val configuration = configure(new Configuration)

    private def configure(c: Configuration): Configuration = {
        c.tap(_.setSourceParserClass(classOf[ResolvingXMLReader].getName))
            .tap(_.setStyleParserClass(classOf[ResolvingXMLReader].getName))
            .tap(_.setURIResolver(new TransientURIResolver(fileSystem)))
            .tap(_.setOutputURIResolver(new TransientOutputURIResolver(fileSystem)))
    }

    /** Convert a sequence of tuples into a [[Map]] of parameters that Saxon understands. */
    def asMap(tuple: (String, Any)*): Map[QName, XdmValue] = Any2Xdm.asMap(tuple:_*)

    /** The in-memory file system that stores [[MockFile]] instances and documents generated with `<xsl:result-document>`.
      *
      * Every specification uses its own in-memory file system. If you need every test to have its own in-memory file
      * system, use the specs2
      * [[https://etorreborre.github.io/specs2/guide/SPECS2-3.8.3/org.specs2.guide.Isolation.html isolation]]
      * argument.
      *
      * Note, however, that if you enable isolation, the stylesheet will currently also be compiled per-test rather than
      * per-specification, because the file system needs to be instantiated before the stylesheet is compiled.
      */
    lazy val fileSystem: FileSystem = Jimfs.newFileSystem(JimfsConfiguration.unix)

    /** An object that contains XPath-related methods. */
    object XPath {
        private lazy val compiler = Saxon.processor.newXPathCompiler

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
            compiler.compile(query).load.tap(_.setContextItem(contextItem)).evaluate.asInstanceOf[XdmNode]
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
            val selector = compiler.compilePattern(query).load
            selector.setContextItem(contextItem)
            selector.effectiveBooleanValue
        }

        def matches(query: String, contextItem: DomNode): Boolean = {
            matches(query, Saxon.builder.wrap(contextItem))
        }
    }

    /** An object for holding instances of Saxon objects. */
    object Saxon {
        /** The Saxon [[Processor]] to use for running the stylesheet.
          *
          * By default, it is configured to use the in-memory file system and an XML parser that can use XML catalogs.
          */
        val processor: Processor = new Processor(configuration)

        /** A Saxon [[DocumentBuilder]] instance for converting DOM nodes into XML Data Model (XDM) instances
          *  that Saxon can use.
          */
        val builder: DocumentBuilder = processor.newDocumentBuilder

        val compiler: XsltCompiler = processor.newXsltCompiler

        /** The compiled XSLT stylesheet.
          *
          * Once compiled, every test in the stylesheet reuses the compiled stylesheet.
          */
        lazy val executable: XsltExecutable = compiler.compile(stylesheet)
    }

    /** Get a new [[Xslt30Transformer]] instance for the compiled stylesheet.
      *
      * Any global [[parameters]] you set are set when this method is called.
      */
    def transformer: Xslt30Transformer = {
        Saxon.executable.load30 tap (_.setStylesheetParameters(parameters))
    }

    /** Given a list of functions that take a [[Jimfs]] instance and return a [[MockFile]], create the mockfiles in the
      * given file system and delete them after running the specification.
      *
      * The mock files are then available for parsing during the test. Any relative URIs and URIs that use
      * [[Jimfs.URI_SCHEME]] as the scheme will be resolved against the given [[Jimfs]] file system.
      */
    case class withFiles(mockFiles: (FileSystem => MockFile)*) extends BeforeAfter {
        def before: Unit = mockFiles.foreach { m => m(fileSystem).create }
        def after: Unit  = mockFiles.foreach { m => m(fileSystem).delete }
    }

    private def application(t: Xslt30Transformer, node: XdmNode): XdmNode => TemplateApplication = {
        new TemplateApplication(t tap (_.setInitialContextItem(node)), _)
    }

    private def loadNode(node: Node): XdmNode = {
        // If [[inputSchema] is defined, use that schema to load the default attributes for the given node.
        inputSchema.map(SchemaAwareXMLLoader(node, _)).getOrElse(TrySuccess(node)) match {
            case TrySuccess(n) => documentNode(n)
            case TryFailure(ex) => throw FailureException(Failure(ex.getMessage))
        }
    }

    /** Apply the XSLT template for the given [[Node]] in the stylesheet defined in this specification.
      *
      * Equivalent to `<xsl:apply-templates/>`.
      */
    def applying(input: => Node): TemplateApplication = {
        val node: XdmNode = loadNode(input)
        application(transformer, node)(node)
    }

    /** Apply the XSLT template for the node selected by the given XPath query, using the input [[Node]] as the context
      * node.
      *
      * Equivalent to `<xsl:apply-templates/>`.
      */
    def applying(query: String)(input: => Node): TemplateApplication = {
        val node: XdmNode = loadNode(input)
        application(transformer, node)(XPath.select(query)(node))
    }

    /** Call an XSLT function with the given parameters. */
    def callingFunction(uri: String, lexical: String)(parameters: Any*): FunctionCall = {
        new FunctionCall(transformer, new QName(uri, lexical), parameters.toArray.map(Any2Xdm.into))
    }

    /** Call an XSLT function, using the namespace URI defined implicitly in the [[XsltSpecification]] instance. */
    def callingFunction(lexical: String)(parameters: Any*)(implicit namespace: String): FunctionCall = {
        callingFunction(namespace, lexical)(parameters:_*)
    }

    /** Call a named XSLT template */
    def callingTemplate(name: String): TemplateCall = new TemplateCall(transformer, new QName(name))

    /** Call a named XSLT template and supply a context node (as [[XdmNode]]) for the transformation. */
    def callingTemplate(name: String, contextNode: XdmNode): TemplateCall = {
        new TemplateCall(transformer tap (_.setInitialContextItem(contextNode)), new QName(name))
    }

    /** Call a named XSLT template and supply a context node (as [[Node]]) for the transformation. */
    def callingTemplate(name: String, contextNode: Node): TemplateCall = callingTemplate(name, element(contextNode))

    private lazy val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    private lazy val document = factory.newDocumentBuilder.newDocument

    /** Create an [[XdmNode]] `attribute()` node.
      *
      * If your template or function expects an attribute, use this function.
      */
    def attribute(a: (String, String)): XdmNode = {
        Saxon.builder.wrap(document.createAttribute(a._1) tap (_.setValue(a._2)))
    }

    /** Convert an [[Elem]] into an [[XdmNode]] `document-node()` node. */
    def documentNode(node: Node): XdmNode = {
        Saxon.builder.build(TransientFileSystem.source(fileSystem, node))
    }

    /** Convert an [[Elem]] into an [[XdmNode]] `element()` node.
      *
      * By default, any [[Elem]] instance you create and give as a parameter is a document node. If your template or
      * function expects an element, use this function.
      */
    def element(node: Node): XdmNode = {
        documentNode(node).axisIterator(Axis.CHILD).toList.reverse.head.asInstanceOf[XdmNode]
    }

    /** Convert a [[ProcInstr]] into an [[XdmNode]] `processing-instruction()` node. */
    def pi(pi: ProcInstr): XdmNode = {
        Saxon.builder.wrap(document.createProcessingInstruction(pi.target, pi.proctext))
    }

    /** Convert a [[String]] into an [[XdmNode]] `text()` node.
      *
      * If you have a function that expects a text node parameter, call this method with a string argument to create
      * a text node, then pass it to your function.
      */
    def text(data: String): XdmNode = Saxon.builder.wrap(document.createTextNode(data))

    /** Create a function that takes a [[Source]] and returns a [[CompareMatcher]] that filters nodes according to the given predicate.
      *
      * For filtering attributes, use [[filterAttr]].
      *
      * Example:
      *
      * {{{
      * // Apply templates for a, ignore differences in the d children of c.
      * applying(<a><b/></a>) must produce(<c/>)(filterNode(!XPath.matches("c/d", _))
      * }}}
      */
    def filterNode(f: DomNode => Boolean): Source => CompareMatcher = {
        defaultMatcher(_).withNodeFilter(new Predicate[DomNode] {
            def test(x: DomNode): Boolean = f(x)
        })
    }

    /** Create a function that takes a [[Source]] and returns a [[CompareMatcher]] that filters attributes according to the given predicate.
      *
      * For filtering elements and other node types, use [[filterNode]].
      *
      * Example:
      *
      * {{{
      * // Apply templates for x, ignore differences in the @id attribute.
      * applying(<x/>) must produce(<y/>)(filterAttr(!XPath.matches("@id", _))
      * }}}
      */
    def filterAttr(f: Attr => Boolean): Source => CompareMatcher = {
        defaultMatcher(_).withAttributeFilter(new Predicate[Attr] {
            def test(x: Attr): Boolean = f(x)
        })
    }
}
