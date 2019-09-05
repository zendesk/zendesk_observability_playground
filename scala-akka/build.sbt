lazy val distProject = project
  .in(file("."))
  .enablePlugins(JavaAgent, JavaAppPackaging)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.1.9",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.9",
      "com.typesafe.akka" %% "akka-stream" % "2.5.23",
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.23",
      // For json logging. https://docs.datadoghq.com/logs/log_collection/java/?tab=slf4j#raw-format
      "net.logstash.logback" % "logstash-logback-encoder" % "5.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
    ),
    scalaVersion := "2.13.0",
    version := "0.1",
    name := "scala-akka",
    dockerExposedPorts := Seq(8090)
  )

