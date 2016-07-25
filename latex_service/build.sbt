name := """latex-service-client"""
organization := "com.sparkydots"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.5.4" % "provided",
  "com.sparkydots" %% "service-discovery" % "1.0-SNAPSHOT"
)
