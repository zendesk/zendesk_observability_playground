# Build base, needs SBT
FROM mozilla/sbt:latest
ADD src /build/src
ADD build.sbt /build/build.sbt
ADD project /build/project
WORKDIR /build
RUN sbt docker:stage

# Run base
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=0 /build/target/docker/stage/opt/docker/bin ./bin
COPY --from=0 /build/target/docker/stage/opt/docker/dd-java-agent ./dd-java-agent
COPY --from=0 /build/target/docker/stage/opt/docker/lib ./lib
CMD ["/app/bin/scala-akka", "-jvm-debug", "8091", "-v", \
#  "-Dcom.sun.management.jmxremote=true", \
#  "-Dcom.sun.management.jmxremote.port=8092", \
#  "-Dcom.sun.management.jmxremote.rmi.port=8092", \
#  "-Dcom.sun.management.jmxremote.local.only=false", \
#  "-Dcom.sun.management.jmxremote.authenticate=false", \
#  "-Dcom.sun.management.jmxremote.ssl=false", \
#  "-Djava.rmi.server.hostname=localhost", \
#  "-Ddd.agent.host=dd-agent", \
#  "-Ddd.trace.agent.port=8126", \
#  "-Ddd.jmxfetch.statsd.host=dd-agent", \
#  "-Ddd.jmxfetch.statsd.port=8125", \
  "-Ddd.service.name=scala-akka", \
  "Ddatadog.slf4j.simpleLogger.defaultLogLevel=debug"]

