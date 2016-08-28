package com.github.eerohele.expek

import net.sf.saxon.s9api.{XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}
import shapeless.syntax.typeable.typeableOps

import scala.collection.JavaConversions.asScalaIterator

/** A trait that represents an XML transformation. Any class that wants to be matched by a [[XsltResultMatchers]] must
  * extends this trait.
  */
trait Transformation {
    /** An unrealized XML transformation that returns an [[XdmValue]]. */
    def transformation: () => XdmValue

    /** The result of the XML transformation converted into a [[Vector]] of native Java types. */
    def result: Vector[Any] = {
        transformation().iterator.toVector.flatMap { x =>
            if (x.isAtomicValue) {
                x.cast[XdmAtomicValue].map(_.getValue)
            } else {
                x.cast[XdmNode].map(identity)
            }
        }
    }
}
