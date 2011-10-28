name := "chained-future"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
      "se.scalablesolutions.akka" % "akka-actor" % "1.2",
      "ch.qos.logback" % "logback-classic" % "0.9.26"
)

fork in (Compile, run) := true
