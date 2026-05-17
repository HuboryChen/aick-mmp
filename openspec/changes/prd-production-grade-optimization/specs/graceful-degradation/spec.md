## ADDED Requirements

### Requirement: Graceful degradation under AI service failure

When the AI analysis service becomes unavailable, the core video surveillance functionality SHALL remain fully operational. AI-dependent features SHALL display a clear "temporarily unavailable" message.

#### Scenario: AI service down

- **WHEN** the AI service is unreachable or returns errors
- **THEN** real-time video streaming SHALL continue without interruption
- **AND** the AI analysis panel SHALL display "分析暂不可用" in the UI
- **AND** camera management, recording playback, and alert browsing SHALL be unaffected

### Requirement: Graceful degradation under Kafka failure

When Kafka becomes unavailable, the alert system SHALL fall back to synchronous API calls to ensure critical alerts are still delivered.

#### Scenario: Kafka broker failure

- **WHEN** Kafka is unavailable for more than 10 seconds
- **THEN** alert events SHALL be sent via direct HTTP API call to the central service
- **AND** events accumulated during the outage SHALL be batched and re-synced once Kafka is restored

### Requirement: Graceful degradation under Redis failure

When Redis becomes unavailable, the system SHALL fall back to local in-memory caches. Rate limiting SHALL be temporarily suspended.

#### Scenario: Redis Sentinel failover

- **WHEN** Redis is unavailable
- **THEN** authentication SHALL fall back to local JWT validation
- **AND** rate limiting SHALL be temporarily disabled
- **AND** stream session tracking SHALL use in-memory fallback

### Requirement: Read-only mode under MySQL failure

When the MySQL primary node fails and no writable replica is available, the system SHALL operate in read-only mode for core browsing features.

#### Scenario: Database read-only mode

- **WHEN** MySQL primary is unavailable and no writable replica exists
- **THEN** users SHALL still be able to browse cameras and recordings
- **AND** users SHALL be able to view real-time video streams
- **AND** configuration changes SHALL be blocked with a clear "system in read-only mode" message
