name := "shop"

version := "0.1"

scalaVersion := "2.12.3"

val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.4",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-remote" % "2.5.4",
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "org.scalatra" %% "scalatra" % "2.6.2",
  "org.http4s" %% "http4s-dsl" % "0.15.0",
  "co.fs2" %% "fs2-core" % "0.9.7",
  "org.scalaj" % "scalaj-http_2.11" % "2.3.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0")
        