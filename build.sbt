name := "akka-chat"

version := "1.0"

description := "A simple chat server/client app demonstrating the Akka actors model"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0",
  "org.scala-lang" % "jline" % "2.10.3"
)

