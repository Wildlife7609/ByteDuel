# One Dockerfile for all three Spring Boot services: build them together, then produce a runtime image for each
# docker-compose.yml picks one with target: backend / matchingsystem / botrunningsystem

# The build stage always uses the build machine's own architecture, without emulation (the jars are architecture-independent)
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
# Maven dependencies are cached by BuildKit, so rebuilds do not download them again
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# The runtime stage only needs a JRE; the memory limit is set by JAVA_TOOL_OPTIONS in docker-compose.yml
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS backend
COPY --from=build /src/backend/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

FROM runtime AS matchingsystem
COPY --from=build /src/matchingsystem/target/matchingsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081

FROM runtime AS botrunningsystem
COPY --from=build /src/botrunningsystem/target/botrunningsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
