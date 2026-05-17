## MODIFIED Requirements

### Requirement: Recording data model

The Recording entity SHALL include an edgeNodeId field to track which edge node produced the recording, enabling fault tracing and storage attribution.

**Change**: Added edgeNodeId field to the Recording data model.

#### Scenario: Recording stored with edge node reference

- **WHEN** a recording file is stored
- **THEN** the system SHALL record the edgeNodeId that produced the recording
- **AND** the edgeNodeId SHALL be queryable in recording search results

#### Scenario: Recording by edge node

- **WHEN** an edge node fails
- **THEN** recordings from that node SHALL be queryable by edgeNodeId
- **AND** the system SHALL display the originating edge node in recording details

## ADDED Requirements

### Requirement: Recording storage capacity calculation

The system SHALL document storage capacity requirements. Baseline: single 1080p@30fps H.265 stream consumes approximately 21.6GB per day; H.264 baseline consumes approximately 43.2GB per day.

#### Scenario: Storage calculation available

- **WHEN** a customer plans deployment
- **THEN** the system SHALL provide a storage calculator or formula
- **AND** the documentation SHALL include per-camera daily storage consumption estimates
