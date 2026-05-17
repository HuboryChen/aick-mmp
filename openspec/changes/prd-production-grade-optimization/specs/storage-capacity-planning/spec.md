## ADDED Requirements

### Requirement: Storage capacity planning documentation

The system SHALL provide storage capacity planning documentation to help customers estimate storage requirements before deployment.

#### Scenario: Storage calculation formula

- **WHEN** a customer requests storage planning guidance
- **THEN** the documentation SHALL provide the formula: `Daily storage per camera = bitrate(bps) × 86400 / 8`
- **AND** SHALL include reference values:
  - 1080p H.265 (2Mbps): ~21.6 GB/day per camera
  - 1080p H.264 (4Mbps): ~43.2 GB/day per camera
  - 720p H.265 (1Mbps): ~10.8 GB/day per camera
  - 720p H.264 (2Mbps): ~21.6 GB/day per camera

#### Scenario: Retention calculation

- **WHEN** calculating total storage for a deployment
- **THEN** the documentation SHALL include: `Total = daily_per_camera × camera_count × retention_days × replica_factor`
- **AND** SHALL note the three-tier storage strategy (hot: SSD 7 days, warm: HDD 30 days, cold: cloud 180 days)

### Requirement: Storage monitoring thresholds

The system SHALL support configurable storage thresholds with clear escalation paths.

#### Scenario: Threshold configuration

- **WHEN** storage usage exceeds 80%
- **THEN** a warning alert SHALL be created
- **WHEN** storage usage exceeds 90%
- **THEN** a critical alert SHALL be created
- **AND** emergency cleanup SHALL trigger (delete oldest cold data first, then reduce retention for non-critical cameras)
