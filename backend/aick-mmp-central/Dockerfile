FROM openjdk:8-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r aick && useradd --no-log-init -r -g aick aick

# Copy the central services JAR
COPY aick-mmp-central/target/aick-mmp-central-*.jar app.jar

# Change ownership of the app directory
RUN chown -R aick:aick /app

# Switch to non-root user
USER aick

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health/readiness || exit 1

# Set JVM options for optimal performance
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]