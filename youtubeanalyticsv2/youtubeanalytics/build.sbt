name := """youtubeanalytics"""
organization := "nextGen"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.15"

libraryDependencies ++= Seq(
  guice,
  ws,
  cacheApi,
  "org.mockito" % "mockito-core" % "3.12.4" % Test,
  "org.mockito" % "mockito-junit-jupiter" % "3.12.4" % Test,
  "org.junit.jupiter" % "junit-jupiter-api" % "5.8.1" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.8.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)
