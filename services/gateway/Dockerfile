# =============================================================================
# IntiFix Gateway — imagen de producción (multi-stage)
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S intifix && adduser -S intifix -G intifix
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER intifix
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8090/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
