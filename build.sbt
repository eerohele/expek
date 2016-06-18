lazy val settings = Seq(
    organization := "com.github.eerohele",
    name := "expek",
    version := "0.1.0",
    scalaVersion := "2.11.8",
    bintrayPackageLabels := Seq("XSLT", "XML", "XPath", "Unit testing"),

    libraryDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5",
      "org.specs2" % "specs2-core_2.11" % "3.8.3",
      "org.xmlunit" % "xmlunit-core" % "2.1.1",
      "org.xmlunit" % "xmlunit-matchers" % "2.1.1",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-5",
      "xml-resolver" % "xml-resolver" % "1.2",
      "com.google.jimfs" % "jimfs" % "1.1",
      "com.chuusai" %% "shapeless" % "2.3.1"
    )
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

lazy val root = (project in file(".")).settings(settings: _*)

scalacOptions in Global ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions"
)

resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
)

autoAPIMappings := true

apiMappings += (
    scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")
)

enablePlugins(SiteScaladocPlugin)

lazy val examples = project
    .dependsOn(root % "compile->test")
    .settings(settings: _*)
    .settings(testOptions in Test += Tests.Setup(() => {
        System.setProperty("xml.catalog.files", "/opt/dita-ot/catalog-dita.xml")
        System.setProperty("specs2.stylesheet.test", "examples/src/test/resources/stylesheets/example.xsl")
      }))
