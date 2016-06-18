Expek
=====

Write unit tests for XSLT in Scala. Why not?

![Using Expek with IntelliJ IDEA](screenshot.png?raw=true "Using Expek with IntelliJ IDEA")

**NOTE**: This library is still in its infancy, so I can't guarantee that the
API is stable yet. Any feedback on how to improve and extend it is warmly
welcomed.

## Example

See [`ExampleSpecification.scala`][example-spec] and
[`example.xsl`][example-stylesheet].

## Documentation

- [API documentation][api]

You might also find the documentation for [specs2][specs2] helpful.

## Use

### SBT

In your `build.sbt`, add:

```scala
libraryDependencies ++= Seq(
  "com.github.eerohele" % "expek_2.11" % "0.1.0"
)
```

Stick tests in `src/test/scala`, run `sbt test`.

### Gradle

In your `build.gradle`, add:

```groovy
repositories {
     maven {
        url "https://oss.sonatype.org/content/repositories/releases"
     }
}

dependencies {
      testCompile 'org.specs2:specs2-junit_2.11:3.8.3'
      testCompile 'com.github.eerohele:expek_2.11:0.1.0'
  }
}
```

Stick tests in `src/test/scala`.

In your Expek specification Scala file, add the `@RunWith` annotation:

```scala
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MySpecification ... { ... }
```

Run `gradle test`.

## Components

- Use Saxon's [Xslt30Transformer][x30t] to apply or call templates and
  functions in the stylesheet.
- Compare expected and actual XML with [XMLUnit 2][xmlunit].
- Run tests with [specs2][specs2].

[api]: https://eerohele.github.io/expek/latest/api
[example-spec]: http://github.com/eerohele/expek/tree/master/examples/src/test/scala/ExampleSpecification.scala
[example-stylesheet]: http://github.com/eerohele/expek/tree/master/examples/src/test/resources/stylesheets/example.xsl
[saxon]: http://www.saxonica.com
[specs2]: http://www.specs2.org
[x30t]: http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/Xslt30Transformer.html
[xmlunit]: https://github.com/xmlunit/xmlunit
