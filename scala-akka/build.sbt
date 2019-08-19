lazy val distProject = project
  .in(file("."))
  .enablePlugins(JavaAgent, JavaAppPackaging)
  .settings(
    javaAgents += "com.datadoghq" % "dd-java-agent" % "0.31.2" % "runtime",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.1.9",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.9",
      "com.typesafe.akka" %% "akka-stream" % "2.5.23",
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.23",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),
    scalaVersion := "2.13.0",
    version := "0.1",
    name := "scala-akka",
    dockerExposedPorts := Seq(8090)
  )
