# Multi-stage so the runtime image carries the jar and nothing else.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Copy the POM first so dependency resolution is cached independently of source
# changes; without this every code edit re-downloads the whole tree.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/loaniq-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
