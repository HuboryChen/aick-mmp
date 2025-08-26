FROM openjdk:8-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick

# Copy the edge services JAR (smaller than central)
COPY aick-mmp-edge/target/aick-mmp-edge-*.jar app.jar

# Change ownership of the app directory
RUN chown -R aick:aick /app

# Switch to non-root user
USER aick

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8081/api/actuator/health || exit 1

# Set JVM options optimized for edge devices with limited resources
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseSerialGC -XX:+UseStringDeduplication -XX:MaxMetaspaceSize=128m"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]