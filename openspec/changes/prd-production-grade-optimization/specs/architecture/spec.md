## ADDED Requirements

### Requirement: Alert entity data model

The system SHALL define an Alert entity as a core data model, supporting independent query, statistics, and archival operations.

#### Scenario: Alert table structure

- **WHEN** the Alert entity is defined
- **THEN** it SHALL contain the following fields:
  - id (Long, primary key, auto-increment)
  - ruleId (Long, foreign key to AlertRule, nullable)
  - cameraId (Long, foreign key to Camera, nullable)
  - level (ENUM: CRITICAL, WARNING, INFO)
  - message (String, 500)
  - detail (TEXT/JSON, nullable, for extended alert context)
  - status (ENUM: UNRESOLVED, ACKNOWLEDGED, RESOLVED)
  - createTime (DateTime)
  - handleTime (DateTime, nullable)
  - handlerId (Long, foreign key to User, nullable)

#### Scenario: Alert table indexes

- **WHEN** the Alert table is created
- **THEN** composite index SHALL be created on (cameraId, createTime)
- **AND** single-column index SHALL be created on level
- **AND** single-column index SHALL be created on (ruleId, status)
