# ============================================================
# CyberAudit7E — Dockerfile multi-stage
# Stage 1 : Build Maven (JDK 25 — aligné sur pom.xml)
# Stage 2 : Runtime minimal (JRE 25)
#
# Bonnes pratiques :
# - Multi-stage (image finale ~200 MB vs ~800 MB)
# - Couche dépendances séparée (cache Docker)
# - Non-root user
# - HEALTHCHECK intégré
# - Labels OCI
# ============================================================

# ── Stage 1 : Build ──
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# Dépendances d'abord (couche mise en cache tant que pom.xml ne change pas)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Code source
COPY src ./src

# Build JAR
RUN mvn clean package -DskipTests -B

# Trouver le JAR exécutable (ignore le -plain.jar / original)
RUN JAR_FILE=$(ls target/*.jar | grep -v original | grep -v plain | head -1) && \
    echo "JAR trouvé : ${JAR_FILE}" && \
    cp "${JAR_FILE}" target/app.jar

# ── Stage 2 : Runtime ──
FROM eclipse-temurin:25

LABEL org.opencontainers.image.title="CyberAudit7E" \
    org.opencontainers.image.description="Moteur d'audit cybernétique — Axiome 7E" \
    org.opencontainers.image.version="1.0.0-M7"

RUN groupadd -r audit && useradd -r -g audit audit && \
    apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/target/app.jar app.jar
RUN chown -R audit:audit /app
USER audit

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -sf http://localhost:8080/api/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE="dev"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
