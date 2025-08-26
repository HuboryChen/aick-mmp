# Multi-stage Dockerfile for AICK-MMP modular build

# Stage 1: Build shared module
FROM maven:3.6.3-openjdk-8-slim AS shared-builder
WORKDIR /build
COPY backend/pom.xml .
COPY backend/aick-mmp-parent ./aick-mmp-parent
COPY backend/aick-mmp-shared ./aick-mmp-shared
COPY backend/aick-mmp-central ./aick-mmp-central
COPY backend/aick-mmp-edge ./aick-mmp-edge
RUN mvn clean install -pl aick-mmp-shared -am -DskipTests

# Stage 2: Build central services
FROM maven:3.6.3-openjdk-8-slim AS central-builder
WORKDIR /build
COPY backend/pom.xml .
COPY backend/aick-mmp-parent ./aick-mmp-parent
COPY backend/aick-mmp-shared ./aick-mmp-shared
COPY backend/aick-mmp-central ./aick-mmp-central
COPY backend/aick-mmp-edge ./aick-mmp-edge
COPY --from=shared-builder /root/.m2 /root/.m2
RUN mvn clean package -pl aick-mmp-central -am -DskipTests

# Stage 3: Build edge services
FROM maven:3.6.3-openjdk-8-slim AS edge-builder
WORKDIR /build
COPY backend/pom.xml .
COPY backend/aick-mmp-parent ./aick-mmp-parent
COPY backend/aick-mmp-shared ./aick-mmp-shared
COPY backend/aick-mmp-central ./aick-mmp-central
COPY backend/aick-mmp-edge ./aick-mmp-edge
COPY --from=shared-builder /root/.m2 /root/.m2
RUN mvn clean package -pl aick-mmp-edge -am -DskipTests

# Stage 4: Central runtime
FROM openjdk:8-jre-slim AS central-runtime
WORKDIR /app
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick
COPY --from=central-builder /build/aick-mmp-central/target/aick-mmp-central-*.jar app.jar
RUN chown -R aick:aick /app
USER aick
EXPOSE 8080
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Stage 5: Edge runtime (default)
FROM openjdk:8-jre-slim AS edge-runtime
WORKDIR /app
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick
COPY --from=edge-builder /build/aick-mmp-edge/target/aick-mmp-edge-*.jar app.jar
RUN chown -R aick:aick /app
USER aick
EXPOSE 8081
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Default to edge runtime
FROM edge-runtime