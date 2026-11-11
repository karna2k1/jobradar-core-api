# syntax=docker/dockerfile:1
# Multi-stage build: Maven builds the jar, a slim JRE runs it. Works on amd64 and arm64 (Oracle Ampere).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
ENV JAVA_OPTS="-Xmx384m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]