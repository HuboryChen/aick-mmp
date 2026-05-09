# Video Wall Settings Persistence

## ADDED Requirements

### Requirement: Layout settings persist across page reloads

The video wall SHALL persist the user's layout selection (1/4/9/16 grids) so that it survives page refresh and navigation to other pages and back.

#### Scenario: Layout persisted after page refresh

- **WHEN** the user changes the layout to a new value via the settings drawer
- **AND** clicks "完成" to confirm
- **AND** then refreshes the page
- **THEN** the video wall SHALL display the last saved layout

#### Scenario: Layout persisted after page navigation

- **WHEN** the user changes the layout via the settings drawer
- **AND** clicks "完成" to confirm
- **AND** navigates to another page
- **AND** navigates back to the video wall page
- **THEN** the video wall SHALL display the last saved layout

### Requirement: Quality and bitrate settings persist across page reloads

The video wall SHALL persist the user's quality and bitrate settings across page navigations.

#### Scenario: Quality persisted after refresh

- **WHEN** the user changes the video quality from 720p to 1080p
- **AND** clicks "完成"
- **AND** refreshes the page
- **THEN** the video wall SHALL use 1080p quality

### Requirement: Camera selection persists across page reloads

The video wall SHALL persist the user's selected camera assignments across page navigations.

#### Scenario: Camera selection persists

- **WHEN** the user assigns specific cameras to video wall cells
- **AND** clicks "完成"
- **AND** navigates away and back
- **THEN** the same cameras SHALL be assigned to the same cells

### Requirement: Local storage as primary data source

The system SHALL use localStorage as the primary data source when loading video wall configuration.

#### Scenario: Local storage takes precedence over database

- **WHEN** the user has both DB-stored preferences and localStorage preferences
- **THEN** the localStorage preferences SHALL be treated as the latest user intent
- **AND** DB preferences SHALL be merged as a fallback for fields not present in localStorage

### Requirement: Immediate database save on explicit confirm

When the user explicitly clicks the "完成" button in the settings drawer, the configuration SHALL be immediately saved to the database without debounce delay.

#### Scenario: Database save on "完成"

- **WHEN** the user clicks "完成" in the settings drawer
- **THEN** the full current configuration SHALL be sent to the backend immediately
- **AND** the page SHALL NOT require a refresh to reflect the saved state
