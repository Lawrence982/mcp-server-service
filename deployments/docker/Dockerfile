# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .

COPY src ./src
RUN mvn package -DskipTests

# ---------- Runtime: HTTP server only (inspector separate) ----------
FROM eclipse-temurin:21-jre-alpine AS http
WORKDIR /app

ARG JAR_FILE=/build/target/*.jar
COPY --from=build $JAR_FILE /app/

EXPOSE 8091
CMD ["sh", "-c", "java -jar /app/*.jar"]