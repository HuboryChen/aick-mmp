## ADDED Requirements

### Requirement: RFC 7807 Problem Details for API errors

All API error responses SHALL conform to RFC 7807 Problem Details format in addition to the existing code/message format. This enables standardized error handling across all API consumers.

#### Scenario: Error response format

- **WHEN** an API error occurs
- **THEN** the response SHALL include RFC 7807 fields:
  - type (URI identifying the problem type)
  - title (short, human-readable summary)
  - status (HTTP status code)
  - detail (human-readable explanation)
  - instance (URI identifying the specific occurrence)
- **AND** the existing code and message fields SHALL be preserved for backward compatibility

#### Scenario: Problem type URI

- **WHEN** the error type is defined
- **THEN** the type URI SHALL use the format: /api/problems/{error-category}/{error-code}
- **AND** a problem type registry SHALL be documented at /api/problems

#### Scenario: Validation errors

- **WHEN** request validation fails
- **THEN** the error response SHALL include an "errors" array with per-field validation messages
- **AND** each validation error SHALL include: field, rejectedValue, message
