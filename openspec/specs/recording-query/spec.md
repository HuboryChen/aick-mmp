# Recording Query

## MODIFIED Requirements

### Requirement: Recording query with enhanced filters

The system SHALL support querying recordings with enhanced filters including status, file size range, and date ranges beyond the current month.

#### Scenario: Query recordings by status
- **WHEN** user queries recordings with status filter
- **THEN** the system SHALL return only recordings matching specified status (COMPLETED, CORRUPTED, PENDING, etc.)

#### Scenario: Query recordings by file size range
- **WHEN** user queries recordings with file size range
- **THEN** the system SHALL return only recordings within specified size range

#### Scenario: Query recordings with pagination
- **WHEN** user requests recording list with pagination
- **THEN** the system SHALL return total count and paginated results
- **AND** support sorting by startTime, endTime, fileSize

### Requirement: Recording metadata display

The system SHALL return complete recording metadata including duration, file size, MD5 checksum, storage path, and integrity status.

#### Scenario: Recording metadata in response
- **WHEN** user queries recording details
- **THEN** the system SHALL return fileSize, duration, md5, storagePath, integrityStatus
