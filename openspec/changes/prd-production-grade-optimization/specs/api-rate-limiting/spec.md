## ADDED Requirements

### Requirement: API rate limiting for all endpoints

The system SHALL enforce rate limits on all API endpoints using a Redis-based sliding window algorithm. Different limits SHALL apply based on authentication level.

#### Scenario: Anonymous endpoint rate limit

- **WHEN** an unauthenticated request is received
- **THEN** the rate limit SHALL be 20 requests per minute per IP
- **AND** exceeding this limit SHALL return HTTP 429

#### Scenario: Authenticated user rate limit

- **WHEN** an authenticated user makes API requests
- **THEN** the rate limit SHALL be 100 requests per minute per user
- **AND** the X-RateLimit-* headers SHALL be included in the response

#### Scenario: Rate limit headers

- **WHEN** a request is within rate limits
- **THEN** the response SHALL include headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
