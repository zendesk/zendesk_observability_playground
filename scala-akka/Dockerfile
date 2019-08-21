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
  "-Ddd.service.name=scala-akka", \
  "Ddatadog.slf4j.simpleLogger.defaultLogLevel=debug"]

