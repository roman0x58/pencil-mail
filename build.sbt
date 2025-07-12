import xerial.sbt.Sonatype.sonatype01

val catsVersion = "2.13.0"
val catsEffectVersion = "3.6.1"
val fs2Version = "3.12.0"
val scodecBitsVersion = "1.2.2"
val scodecCoreVersion = "2.3.2"
val specs2Version = "4.19.2"
val scalacheckVersion = "1.18.1"
val log4catsVersion = "2.7.1"
val logbackVersion = "1.5.18"
val literallyVersion = "1.2.0"
val http4sVersion = "0.23.30"
val circeVersion = "0.14.14"
val testContainersVersion = "1.21.3"

lazy val root = (project in file("."))
  .settings(
    version := "3.0.2",
    organization := "io.github.roman0x58",
    name := "pencil-mail",
    scalaVersion := "3.7.1",
    scalacOptions ++= Seq(
      "-language:experimental.macros",
      "-indent",
      "-source:future",
      "-deprecation",
      "-feature"
    ),
    javacOptions ++= Seq("-source", "1.17", "-target", "1.17"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "literally" % literallyVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "co.fs2" %% "fs2-scodec" % fs2Version % Test,
      "org.scodec" %% "scodec-core" % scodecCoreVersion,
      "org.scodec" %% "scodec-bits" % scodecBitsVersion,
      "org.typelevel" %% "log4cats-core" % log4catsVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion % Test,
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
      "org.testcontainers" % "testcontainers" % testContainersVersion % Test,
      "org.http4s" %% "http4s-ember-client" % http4sVersion % Test,
      "org.http4s" %% "http4s-dsl" % http4sVersion % Test,
      "org.http4s" %% "http4s-circe" % http4sVersion % Test,
      "io.circe" %% "circe-core" % circeVersion % Test,
      "io.circe" %% "circe-generic" % circeVersion % Test,
      "io.circe" %% "circe-parser" % circeVersion % Test
    ),
    licenses += ("Apache-2.0", url(
      "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ))
  )

