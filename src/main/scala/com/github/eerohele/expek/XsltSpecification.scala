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
import org.specs2.mutable.BeforeAfter
import org.w3c.dom.Node
import org.xmlunit.matchers.CompareMatcher
import org.xmlunit.matchers.CompareMatcher.isSimilarTo
import org.xmlunit.util.Predicate

import scala.collection.JavaConversions.{asScalaIterator, mapAsJavaMap}
import scala.xml.{Elem, ProcInstr}

private[expek] sealed abstract class Parametrized(transformer: Xslt30Transformer) {
    def withParameters(tunnel: Boolean, parameters: (String, Any)*): this.type = {
        withParameters(tunnel, Any2Xdm.asMap(parameters:_*))
    }

    def withParameters(tunnel: Boolean, parameters: Map[QName, XdmValue]): this.type = {
        transformer.setInitialTemplateParameters(parameters, tunnel)
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

trait XsltSpecification extends XsltResultMatchers {
    import utils.Tap

    /** The stylesheet you want to test. */
    val stylesheet: Source

    /** Functions for converting an XSLT stylesheet into a [[Source]]. */
    object XSLT {
        import NodeConversions.nodeToString

        /** Read a stylesheet from a file. */
        def file(xslt: String): Source = new StreamSource(new File(xslt))

        /** Read a stylesheet from an [[Elem]]. */
        def elem(elem: Elem): Source = new StreamSource(new StringReader(elem))
    }

    /** The default matcher for comparing two XML element or document nodes. */
    val defaultMatcher: Source => CompareMatcher = s => isSimilarTo(s).normalizeWhitespace

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
          *     applying(
          *         // The ancestor element is set as the context node for the transformation.
          *         <ancestor copied="value"><descendant/></ancestor>,
          *         // Use XPath to select the element that you want to apply the templates for.
          *         XPath.select("ancestor/descendant")
          *     ) must produce (<descendant copied="value"/>)
          * }
          * }}}
          */
        def select(query: String)(contextItem: XdmItem): XdmNode = {
            compiler.compile(query).load.tap((_.setContextItem(contextItem))).evaluate.asInstanceOf[XdmNode]
        }

        def matches(query: String, contextItem: XdmItem): Boolean = {
            val selector = compiler.compilePattern(query).load
            selector.setContextItem(contextItem)
            selector.effectiveBooleanValue
        }

        def matches(query: String, contextItem: Node): Boolean = {
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

        private val compiler: XsltCompiler = processor.newXsltCompiler

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

    /** Apply the XSLT template for the given [[Elem]] in the stylesheet defined in this specification.
      *
      * Equivalent to `<xsl:apply-templates/>`.
      */
    def applying(input: Elem): TemplateApplication = {
        val node: XdmNode = documentNode(input)
        application(transformer, node)(node)
    }

    def applying(input: Elem, query: XdmNode => XdmNode): TemplateApplication = {
        val node: XdmNode = documentNode(input)
        application(transformer, node)(query(node))
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

    /** Call a named XSLT template and supply a context node for the transformation. */
    def callingTemplate(name: String, contextNode: Elem): TemplateCall = {
        transformer.setInitialContextItem(documentNode(contextNode))
        callingTemplate(name)
    }

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
    def documentNode(elem: Elem): XdmNode = {
        Saxon.builder.build(TransientFileSystem.source(fileSystem, elem))
    }

    /** Convert an [[Elem]] into an [[XdmNode]] `element()` node.
      *
      * By default, any [[Elem]] instance you create and give as a parameter is a document node. If your template or
      * function expects an element, use this function.
      */
    def element(elem: Elem): XdmNode = {
        documentNode(elem).axisIterator(Axis.CHILD).toList.reverse.head.asInstanceOf[XdmNode]
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

    /** Create an XMLUnit node filter predicate.
      *
      * Java doesn't have HOFs, so the default XMLUnit syntax for defining predicates is somewhat awkward. This is an
      * attempt to make it more idiomatic Scala.
      *
      * Example:
      *
      * {{{
      * // Create a filter that ignores @id attributes.
      * val f = filter[Attr](a => a.getName != "id")
      *
      * // Use it.
      * val m = (s: Source) => defaultMatcher(s).withAttributeFilter(f)
      *
      * // Apply templates for <x/>, use the @id-ignoring matcher when comparing the expected and actual results.
      * applying(<x/>) must produce(<y/>)(m)
      * }}}
      */
    def filter[T <: Node](f: T => Boolean): Predicate[T] = {
        new Predicate[T] {
            def test(x: T): Boolean = f(x)
        }
    }

    def exclude[T <: Node](f: T => Boolean): Predicate[T] = filter(!f(_))
}
