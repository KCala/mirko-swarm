name := "mirko-swarm"
organization := "me.kcala"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.8",
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "io.spray" %% "spray-json" % "1.3.4",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.8",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "org.apache.logging.log4j" % "log4j-api" % "2.10.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.10.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.10.0",
)

fork in run := true