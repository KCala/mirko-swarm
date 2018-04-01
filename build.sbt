import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory
import sbtfrontend.FrontendPlugin.autoImport.FrontendKeys._

enablePlugins(FrontendPlugin)
enablePlugins(PackPlugin)

name := "mirko-swarm"
organization := "kcala"

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

lazy val frontendDir = settingKey[File]("Base directory for the frontend")
frontendDir := baseDirectory.value / "frontend"

lazy val resourcesDir = settingKey[File]("External resources directory")
lazy val prepareResourcesDir = settingKey[Unit]("Creade external resources directory")
resourcesDir := (Compile / target).value / "resources"
unmanagedResourceDirectories in Compile += resourcesDir.value
prepareResourcesDir := {
  IO.createDirectory(resourcesDir.value)
}

lazy val frontendTargetDir = settingKey[File]("Frontend assets target directory")
frontendTargetDir := resourcesDir.value / "frontend"
cleanFiles += frontendTargetDir.value

nodePackageManager := sbtfrontend.NodePackageManager.Yarn
frontendFactory := new FrontendPluginFactory(frontendDir.value, baseDirectory.value / "tmp" / "frontendBin")

lazy val prepareFrontendDir = taskKey[Unit]("Create frontend target directory")
val buildFrontend = taskKey[Unit]("Build frontend bundle")

prepareFrontendDir := {
  prepareResourcesDir.value
  IO.createDirectory(frontendTargetDir.value)
}

buildFrontend := {
  prepareFrontendDir.value
  yarn.toTask(" build").value
  IO.listFiles(frontendDir.value / "dist").foreach(file => {
    println(s"Copying $file to ${frontendTargetDir.value / file.name}")
    IO.move(file, frontendTargetDir.value / file.name)
  })
}

packGenerateWindowsBatFile := false
packMain := Map("mirko-swarm" -> "kcala.mirkoSwarm.main.Main")
//packExtraClasspath += Map("mirko-swarm" -> Seq("${PROG_HOME}/resources"))
packResourceDir += (resourcesDir.value -> "resources")

lazy val build = taskKey[Unit]("Build the distributable, self-contained package with executable.")
build := {
  Def.sequential(
    buildFrontend,
    pack
  ).value
}
