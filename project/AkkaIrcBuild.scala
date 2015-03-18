import sbt._
import Keys._

object AkkaIrcBuild extends Build {

  lazy val sharemapProject = Project(id = "datamap", base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "akka-chat",
      version := "1.0",
      description := "A simple chat server/client app demonstrating the Akka actors model",
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      scalaVersion := "2.10.3",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.3.0",
        "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
        "com.typesafe.akka" %% "akka-remote" % "2.3.0",
        "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0",
        "org.scala-lang" % "jline" % "2.10.3",
        "joda-time" % "joda-time" % "2.3",
        "org.joda" % "joda-convert" % "1.6"
      )
    ))
}
