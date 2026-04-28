# =============================================================
# Stage 1 — BUILD
# Uses the full Maven + JDK 21 image to compile & package the jar.
# The /root/.m2 cache layer is preserved between builds when the
# pom.xml hasn't changed, which keeps CI fast.
# =============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy dependency descriptor first (maximises Docker layer cache)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download all dependencies before copying source
RUN ./mvnw dependency:go-offline -q

# Copy source and build (skip tests — tests run in a separate CI step)
COPY src src
RUN ./mvnw package -DskipTests -q

# Extract Spring Boot layered jar for optimal image layering
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# =============================================================
# Stage 2 — RUNTIME
# Minimal JRE 21 Alpine image (~80 MB).  Only the extracted
# Spring Boot layers are copied over — no JDK, no Maven.
# =============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy Spring Boot extracted layers in dependency order
# (least-frequently-changed layers first for best cache reuse)
COPY --chown=appuser:appgroup --from=builder /workspace/target/extracted/dependencies          ./
COPY --chown=appuser:appgroup --from=builder /workspace/target/extracted/spring-boot-loader    ./
COPY --chown=appuser:appgroup --from=builder /workspace/target/extracted/snapshot-dependencies ./
COPY --chown=appuser:appgroup --from=builder /workspace/target/extracted/application           ./

# App listens on 8080 by default; override with SERVER_PORT env var if needed
EXPOSE 8080

# JVM tuning for t2.medium (2 vCPU / 4 GB RAM):
#   -XX:MaxRAMPercentage=75  → cap heap at 75 % of container memory
#   -XX:+UseZGC              → low-pause GC well-suited to request workloads
#   -Djava.security.egd      → faster SecureRandom startup
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "org.springframework.boot.loader.launch.JarLauncher"]

