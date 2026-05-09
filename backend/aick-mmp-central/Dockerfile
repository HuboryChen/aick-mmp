# Multi-stage: build + runtime for Central Service
# Uses BuildKit --mount=type=cache for Maven repository persistence

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy all source
COPY aick-mmp-parent aick-mmp-parent/
COPY aick-mmp-shared aick-mmp-shared/
COPY aick-mmp-central aick-mmp-central/

# Build: install parent -> install shared -> package central
# --mount=type=cache persists Maven repo across builds (pom changes won't re-download everything)
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -f aick-mmp-parent/pom.xml install -N -DskipTests -B && \
    mvn -f aick-mmp-shared/pom.xml install -DskipTests -B && \
    mvn -f aick-mmp-central/pom.xml package -Dmaven.test.skip=true -B

# ============================================================
# Runtime stage
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r aick && useradd --no-log-init -r -g aick aick

COPY --from=build /build/aick-mmp-central/target/aick-mmp-central-*.jar app.jar

RUN chown -R aick:aick /app
USER aick

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health/readiness || exit 1

ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
