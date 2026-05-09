# Video Recording Storage

## ADDED Requirements

### Requirement: Recording file storage path configuration

The system SHALL support configurable recording storage base path. Recording files SHALL be organized by date in the format `{basePath}/YYYY/MM/DD/{cameraId}/{filename}`.

### Requirement: Recording file integrity verification

The system SHALL calculate MD5 checksum for each recording file upon storage completion. The MD5 checksum SHALL be stored in the database alongside the recording metadata.

#### Scenario: Recording file stored with MD5
- **WHEN** a recording file is successfully stored
- **THEN** the system SHALL calculate MD5 checksum
- **AND** store the checksum in the recording metadata record

#### Scenario: Integrity check passed
- **WHEN** user requests integrity verification for a recording
- **AND** the stored MD5 matches the current file MD5
- **THEN** the system SHALL return verification success

#### Scenario: Integrity check failed
- **WHEN** user requests integrity verification for a recording
- **AND** the stored MD5 does NOT match the current file MD5
- **THEN** the system SHALL mark the recording as corrupted
- **AND** create an alert for system administrator

### Requirement: Recording file status tracking

The system SHALL track recording file status. Status values include: PENDING, RECORDING, COMPLETED, CORRUPTED, DELETED.

#### Scenario: Recording status transition to COMPLETED
- **WHEN** a recording session ends
- **AND** the recording file passes integrity verification
- **THEN** the system SHALL set recording status to COMPLETED

#### Scenario: Recording file locked during download
- **WHEN** a recording file is being downloaded
- **THEN** the system SHALL mark the file as locked
- **AND** prevent deletion of locked files

### Requirement: Recording storage capacity monitoring

The system SHALL monitor storage capacity and trigger alerts when usage exceeds configured thresholds (80% warning, 90% critical).

#### Scenario: Storage capacity warning
- **WHEN** storage usage exceeds 80% of configured capacity
- **THEN** the system SHALL create a warning alert

#### Scenario: Storage capacity critical
- **WHEN** storage usage exceeds 90% of configured capacity
- **THEN** the system SHALL create a critical alert
- **AND** trigger emergency cleanup of oldest recordings
