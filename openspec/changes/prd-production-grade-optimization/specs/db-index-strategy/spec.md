## ADDED Requirements

### Requirement: Database index strategy

The system SHALL define and document a database index strategy covering all core query paths, ensuring query performance under 1000+ camera deployments.

#### Scenario: Indexes defined

- **WHEN** the database schema is deployed
- **THEN** the following indexes SHALL exist:
  - camera(name) UNIQUE
  - camera(edge_node_id, status)
  - recording(camera_id, start_time, type)
  - recording(start_time)
  - alert(camera_id, create_time)
  - alert(rule_id, status)
  - edge_node(region_id, status)
  - stream_session(camera_id, status)
  - stream_session(user_id)

#### Scenario: Index migration

- **WHEN** an index is added
- **THEN** it SHALL be created in a new migration script
- **AND** the migration SHALL use CONCURRENTLY or CREATE INDEX IF NOT EXISTS where supported
- **AND** index creation SHALL be scheduled during low-traffic periods

### Requirement: Query performance baseline

The system SHALL define query performance baselines (P99 < 500ms) for all indexed query paths.

#### Scenario: Performance baseline met

- **WHEN** monitoring the database query performance
- **THEN** P99 query latency for indexed queries SHALL be below 500ms
- **AND** queries without index coverage SHALL trigger a warning alert
