package com.github.eerohele.expek

package object utils {
    private[expek] implicit class Tap[A](private val a: A) extends AnyVal {
        def tap[B](f: A => Unit): A = { f(a); a }
    }
}
