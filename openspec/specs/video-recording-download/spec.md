# Video Recording Download

## ADDED Requirements

### Requirement: Single recording download

The system SHALL allow users to download a single recording file. The download SHALL include proper filename and support resume capability.

#### Scenario: Successful single recording download
- **WHEN** user clicks download button for a single recording
- **THEN** the system SHALL initiate download with correct filename
- **AND** return the recording file as an attachment

#### Scenario: Download with integrity verification
- **WHEN** user downloads a recording with integrity verification enabled
- **THEN** the system SHALL include the MD5 checksum in response header `Content-MD5`

### Requirement: Batch recording download

The system SHALL support downloading multiple recordings as a ZIP archive. The ZIP file SHALL contain all selected recordings with their original filenames preserved.

#### Scenario: Successful batch download
- **WHEN** user selects multiple recordings
- **AND** clicks download button
- **THEN** the system SHALL create a ZIP archive
- **AND** return the archive as download

#### Scenario: Batch download size limit
- **WHEN** user selects recordings exceeding 2GB total size
- **THEN** the system SHALL return an error message
- **AND** suggest reducing selection

### Requirement: Download progress tracking

The system SHALL provide download progress information for large files. Progress SHALL be trackable via a dedicated endpoint.

#### Scenario: Download progress query
- **WHEN** user requests download progress for an active download session
- **THEN** the system SHALL return bytes downloaded and total bytes

### Requirement: Download speed limit

The system SHALL support configurable download speed limits per user to prevent bandwidth exhaustion.

#### Scenario: Download speed throttling
- **WHEN** a download is in progress
- **AND** user has exceeded speed quota
- **THEN** the system SHALL reduce download bandwidth
- **AND** return appropriate chunk size

### Requirement: Concurrent download limit

The system SHALL limit concurrent downloads per user to 3 maximum. Additional download requests SHALL be queued or rejected.

#### Scenario: Exceeding concurrent download limit
- **WHEN** user already has 3 active downloads
- **AND** initiates a new download
- **THEN** the system SHALL reject the request
- **AND** return an error message indicating limit reached
