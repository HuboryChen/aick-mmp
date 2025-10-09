FROM openjdk:8-jre-slim AS central-runtime
WORKDIR /app
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick
COPY backend/aick-mmp-central/target/aick-mmp-central-*.jar app.jar
RUN chown -R aick:aick /app
USER aick
EXPOSE 8080
ENV JAVA_OPTS="-Xmx1g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Stage 3: Edge runtime
FROM openjdk:8-jre-slim AS edge-runtime
WORKDIR /app
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick
COPY backend/aick-mmp-edge/target/aick-mmp-edge-*.jar app.jar
RUN chown -R aick:aick /app
USER aick
EXPOSE 8080
ENV JAVA_OPTS="-Xmx1g -Xms1g -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Default to edge runtime
FROM edge-runtime