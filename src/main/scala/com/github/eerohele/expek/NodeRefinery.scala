package com.github.eerohele.expek

import scala.xml.transform.RewriteRule
import scala.xml.{Elem, NamespaceBinding, Node, TopScope}

/** A container class for functions that refine [[Node]] instances in some way.
  *
  * "Refine" here means "modify in some way before or after the XSLT transformation.
  */
object NodeRefinery {
    /** Add a namespace to a [[Node]].
      *
      * Example:
      *
      * {{{
      * setNameSpace("urn:foo:bar")(<foo/>)
      * }}}
      */
    def setNameSpace(uri: String)(node: Node): Node = {
        object Modify extends RewriteRule {
            val binding = NamespaceBinding(None.orNull, uri, TopScope)

            override def transform(n: Node): Seq[Node] = n match {
                case e: Elem => e.copy(scope = binding, child = e.child.map(Modify))
                case n: Node => n
            }
        }

        Modify(node)
    }
}
