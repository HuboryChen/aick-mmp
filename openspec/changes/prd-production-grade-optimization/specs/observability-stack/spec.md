## ADDED Requirements

### Requirement: Prometheus metrics endpoint

The system SHALL expose metrics in Prometheus format via Spring Boot Actuator and Micrometer. All backend services SHALL expose a /actuator/prometheus endpoint.

#### Scenario: Metrics endpoint available

- **WHEN** the backend service is running
- **THEN** GET /actuator/prometheus SHALL return metrics in Prometheus text format
- **AND** the endpoint SHALL be secured (accessible only from internal network)

#### Scenario: JVM metrics exposed

- **WHEN** Prometheus scrapes the metrics endpoint
- **THEN** JVM metrics (heap, non-heap, GC, thread count) SHALL be available
- **AND** HTTP request metrics (count, duration, status distribution) SHALL be available

### Requirement: Grafana dashboard

The system SHALL provide a Grafana dashboard template for visualizing system health, performance, and resource utilization metrics.

#### Scenario: Dashboard available

- **WHEN** a new system is deployed
- **THEN** a pre-configured Grafana dashboard SHALL be available
- **AND** the dashboard SHALL include panels for: service health, API latency (P50/P95/P99), video stream latency, edge node heartbeats, storage usage

### Requirement: Structured logging

The system SHALL output logs in structured JSON format (ELK-compatible) for centralized log aggregation.

#### Scenario: JSON log format

- **WHEN** the application produces a log entry
- **THEN** the log SHALL include: @timestamp, level, logger, message, requestId, userId (if available), duration (if applicable)
- **AND** logs SHALL be written to a separate application-json.log file
