## ADDED Requirements

### Requirement: Feature flag system

The system SHALL provide a feature flag mechanism to control feature availability at runtime without deployment. Feature states SHALL be stored in the database with a local cache (TTL 30 seconds) for performance.

#### Scenario: Feature flag defined

- **WHEN** a new AI feature is added
- **THEN** it SHALL be registered in the feature flag registry
- **AND** each flag SHALL support: enabled (boolean), scope (GLOBAL/REGION/CAMERA), description, owner, created_at, updated_at

#### Scenario: Kill switch activated

- **WHEN** a mission-critical issue is detected in a feature
- **THEN** an operator SHALL set the feature flag to disabled
- **AND** the system SHALL stop using the feature within 30 seconds
- **AND** no deployment or restart SHALL be required

### Requirement: API version deprecation policy

API versions SHALL follow a deprecation lifecycle with minimum 6 months of backward compatibility.

#### Scenario: API version deprecated

- **WHEN** an API version is deprecated
- **THEN** the response SHALL include `Sunset` and `Deprecated` headers
- **AND** the deprecation SHALL be documented in the API changelog
- **AND** the version SHALL remain functional for at least 6 months after deprecation notice
