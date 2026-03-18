# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests

# ---------- Runtime: HTTP server only (inspector separate) ----------
FROM eclipse-temurin:21-jre-alpine AS http
WORKDIR /app

COPY --from=build /build/target/mcp-server-service-2.0-SNAPSHOT.jar /app/app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8091
CMD ["java", "-jar", "/app/app.jar"]
