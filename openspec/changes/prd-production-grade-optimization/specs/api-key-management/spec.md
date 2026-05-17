## ADDED Requirements

### Requirement: API Key rate limiting

API Keys SHALL have configurable rate limits to prevent abuse. Rate limits SHALL be enforced per API Key, with different tiers for different key types.

#### Scenario: Rate limit enforced

- **WHEN** an API Key exceeds its rate limit
- **THEN** the system SHALL return HTTP 429 (Too Many Requests)
- **AND** include a Retry-After header indicating when the client can retry

#### Scenario: Rate limit tiers

- **WHEN** an API Key is created
- **THEN** the default rate limit SHALL be 1000 requests per minute
- **AND** the limit SHALL be configurable per API Key

#### Scenario: Rate limit metrics exposed

- **WHEN** the rate limiter is active
- **THEN** the system SHALL expose rate limit metrics (requests count, throttled count, remaining quota) via Prometheus
