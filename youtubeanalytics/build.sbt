name := """youtubeAnalytics"""
organization := "nextgen"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.15"

libraryDependencies ++= Seq(
  guice,
  "com.google.apis" % "google-api-services-youtube" % "v3-rev222-1.25.0",
  ws,
  cacheApi,
  caffeine,
  "org.mockito" % "mockito-core" % "3.12.4" % Test,
  "org.mockito" % "mockito-junit-jupiter" % "3.12.4" % Test,
  "org.junit.jupiter" % "junit-jupiter-api" % "5.8.1" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.8.1" % Test,
   "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)

Test / testOptions += Tests.Argument(TestFrameworks.JUnit)

addCommandAlias("javadoc", "doc")
