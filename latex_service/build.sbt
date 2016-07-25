import play.sbt.PlayScala

name := """latex-service-client"""
organization := "com.sparkydots"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += ws

libraryDependencies += "com.sparkydots" %% "service-discovery" % "1.0-SNAPSHOT"

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main" / "scala"
