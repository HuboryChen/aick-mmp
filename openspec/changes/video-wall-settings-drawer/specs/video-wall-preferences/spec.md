# Video Wall Preferences

## ADDED Requirements

### Requirement: Preference persistence (dual sync)

The system SHALL automatically persist user preferences to both localStorage and the database whenever the user makes changes, with debouncing to prevent excessive writes.

#### Scenario: Save layout preference (dual sync)
- **WHEN** user changes the video wall layout
- **THEN** the new layout value is saved to localStorage immediately
- **AND** the change is debounced (500ms) and saved to the database

#### Scenario: Save quality preference (dual sync)
- **WHEN** user changes the video quality setting
- **THEN** the new quality value is saved to localStorage immediately
- **AND** the change is debounced (500ms) and saved to the database

#### Scenario: Save bitrate preference (dual sync)
- **WHEN** user adjusts the bitrate slider
- **THEN** the new bitrate value is saved to localStorage immediately
- **AND** the change is debounced (500ms) and saved to the database

### Requirement: Preference debouncing

The system SHALL debounce database writes to prevent excessive requests during rapid changes.

#### Scenario: Multiple changes within debounce window
- **WHEN** user makes multiple changes within 500 milliseconds
- **THEN** only the final state is saved to the database
- **AND** intermediate states are saved to localStorage only

### Requirement: Preference loading with built-in fallback

The system SHALL load user preferences on page load with the following priority:
1. Database (if authenticated)
2. localStorage (if database unavailable)
3. Built-in system presets (if both unavailable)

#### Scenario: Load preferences from database
- **WHEN** the video wall page loads
- **AND** the user is authenticated
- **AND** the database returns valid preferences
- **THEN** preferences are loaded from the database
- **AND** the video wall is configured with the saved preferences

#### Scenario: Fallback to localStorage
- **WHEN** the video wall page loads
- **AND** the database request fails or returns empty
- **THEN** preferences are loaded from localStorage
- **AND** the video wall is configured with the localStorage values

#### Scenario: Fallback to built-in presets
- **WHEN** the video wall page loads
- **AND** both database and localStorage are unavailable or empty
- **THEN** the built-in "四分屏" preset is applied as default
- **AND** no preference data is stored yet

#### Scenario: User preferences override built-in presets
- **WHEN** the video wall page loads
- **AND** the user has saved preferences in database or localStorage
- **THEN** user preferences take precedence over built-in presets
- **AND** the video wall is configured with user preferences

### Requirement: Unauthenticated user preferences

The system SHALL store preferences in localStorage for unauthenticated users, without database persistence.

#### Scenario: Anonymous user saves preferences
- **WHEN** an unauthenticated user changes preferences
- **THEN** the preferences are saved to localStorage only
- **AND** no database request is made

#### Scenario: Anonymous user logs in
- **WHEN** an anonymous user with localStorage preferences logs in
- **THEN** if the user has no database preferences, localStorage preferences are synced to database

### Requirement: Preference data structure

The preference data SHALL include layout, quality, bitrate, selected camera IDs, last applied preset ID, and auto-apply flag.

#### Scenario: Preference contains all fields
- **WHEN** preferences are saved
- **THEN** the saved data includes layout, quality, bitrate, cameraIds, lastPresetId, and autoApply fields
- **AND** all fields are stored in both localStorage and database

### Requirement: Cross-device sync

The system SHALL sync user preferences across devices when the user logs in.

#### Scenario: Sync preferences on login
- **WHEN** user logs in on a different device
- **THEN** the preferences from the database are loaded
- **AND** localStorage is updated with the database values

#### Scenario: Local changes conflict with remote
- **WHEN** user has local changes in localStorage
- **AND** logs in on a new device
- **THEN** database preferences take precedence
- **AND** localStorage is overwritten with database values

### Requirement: Last preset tracking

The system SHALL track which preset was last applied by the user.

#### Scenario: Track last applied preset
- **WHEN** user applies a preset
- **THEN** the preset ID is saved as lastPresetId in preferences
- **AND** the video wall displays the preset as active

#### Scenario: Clear last preset on manual change
- **WHEN** user manually changes any setting
- **THEN** the lastPresetId is set to null
- **AND** the preset list shows no active preset
