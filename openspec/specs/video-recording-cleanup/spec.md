# Video Recording Cleanup

## ADDED Requirements

### Requirement: Time-based retention policy

The system SHALL automatically delete recordings older than the configured retention period (default: 30 days). The retention period SHALL be configurable per camera or globally.

#### Scenario: Automatic cleanup of expired recordings
- **WHEN** cleanup job runs
- **AND** a recording exceeds retention period
- **THEN** the system SHALL delete the recording file
- **AND** mark the recording as DELETED in database

#### Scenario: Per-camera retention override
- **WHEN** a camera has custom retention setting
- **THEN** the system SHALL use camera-specific retention period
- **AND** ignore global retention setting for that camera

### Requirement: Manual cleanup trigger

The system SHALL allow administrators to manually trigger cleanup process. Administrators SHALL be able to specify cleanup scope (all recordings, specific cameras, specific time range).

#### Scenario: Manual cleanup for specific cameras
- **WHEN** admin triggers cleanup for specific cameras
- **THEN** the system SHALL only delete recordings for specified cameras
- **AND** respect retention period

### Requirement: Cleanup safety checks

The system SHALL perform safety checks before deleting recordings. Recordings that are currently being downloaded or have active playback sessions SHALL NOT be deleted.

#### Scenario: Protected recording during active download
- **WHEN** cleanup job runs
- **AND** a recording is being downloaded
- **THEN** the system SHALL skip that recording
- **AND** log the skip reason

#### Scenario: Protected recording during active playback
- **WHEN** cleanup job runs
- **AND** a recording is in active playback session
- **THEN** the system SHALL skip that recording
- **AND** mark it for cleanup in next cycle

### Requirement: Cleanup job scheduling

The system SHALL support configurable cleanup job schedule. Default schedule SHALL be daily at 02:00 AM.

#### Scenario: Scheduled cleanup execution
- **WHEN** cleanup scheduled time is reached
- **THEN** the system SHALL execute cleanup job
- **AND** log cleanup results

### Requirement: Cleanup audit logging

The system SHALL log all cleanup operations with details: what was deleted, when, why (auto/manual), and who triggered it.

#### Scenario: Cleanup audit log entry
- **WHEN** a cleanup operation completes
- **THEN** the system SHALL create an audit log entry
- **AND** include recording IDs, file sizes, timestamps, and trigger type
