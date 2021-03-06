lazy val settings = Seq(
    organization := "com.github.eerohele",
    name := "expek",
    version := "0.2.0",
    scalaVersion := "2.11.8",
    bintrayPackageLabels := Seq("XSLT", "XML", "XPath", "Unit testing"),

    libraryDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5",
      "org.specs2" % "specs2-core_2.11" % "3.8.3",
      "org.xmlunit" % "xmlunit-core" % "2.2.1",
      "org.xmlunit" % "xmlunit-matchers" % "2.2.1",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-5",
      "xml-resolver" % "xml-resolver" % "1.2",
      "com.google.jimfs" % "jimfs" % "1.1",
      "com.chuusai" %% "shapeless" % "2.3.1"
    ),

    testOptions in Test += Tests.Setup(() => {
      System.setProperty("specs2.stylesheet.test", "examples/src/test/resources/stylesheets/example.xsl")
    })
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

ghpages.settings

git.remoteRepo := "git@github.com:eerohele/expek.git"
