import sbt._
import sbt.Keys._

object RunnerBuild extends Build {

  lazy val foo = Project(
    id = "foo",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "foo",
      organization := "org.example",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0-RC3",
      // add other settings here
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.1.0-RC3" cross CrossVersion.full,
        "com.typesafe.akka" %% "akka-zeromq" % "2.1.0-RC3" cross CrossVersion.full,
        "org.apache.thrift" % "libthrift" % "0.9.0",
        "org.slf4j" % "slf4j-log4j12" % "1.5.8"
      ),
      javaSource in Compile <<= (sourceDirectory in Compile)(_ / "thrift/gen-java")
    )
  )
}
