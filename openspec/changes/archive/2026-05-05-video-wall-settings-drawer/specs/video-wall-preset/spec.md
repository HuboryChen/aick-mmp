# Video Wall Preset

## ADDED Requirements

### Requirement: System built-in presets

The system SHALL provide hardcoded built-in presets as fallback options that cannot be deleted.

#### Scenario: Display built-in presets
- **WHEN** the user opens the preset list
- **THEN** the built-in presets (单屏监控, 四分屏, 九宫格) are always displayed at the top of the list
- **AND** built-in presets are visually distinguished (e.g., icon indicator)

#### Scenario: Built-in presets cannot be deleted
- **WHEN** user attempts to delete a built-in preset
- **THEN** the delete option is disabled or hidden
- **AND** a tooltip explains that system presets cannot be deleted

### Requirement: Preset list display

The system SHALL display a list of presets in the preset selector section, showing the preset name and whether it is currently active.

#### Scenario: Display preset list
- **WHEN** the user opens the settings drawer
- **THEN** all presets (built-in + user-created) are displayed in a vertical list
- **AND** each preset shows its name
- **AND** the active preset is highlighted

#### Scenario: Empty user preset list
- **WHEN** the user has not created any presets
- **THEN** the user-created section shows an empty state
- **AND** prompts the user to create their first preset

### Requirement: Apply preset

The system SHALL allow users to apply a preset, which replaces all current video wall settings with the preset's configuration.

#### Scenario: Apply preset successfully
- **WHEN** user clicks on a preset in the list
- **THEN** the layout, quality, bitrate, and camera selection update to match the preset
- **AND** the preset is marked as active
- **AND** the changes are persisted

#### Scenario: Apply preset with camera IDs
- **WHEN** user applies a preset that includes camera IDs
- **THEN** the video wall displays only the specified cameras
- **AND** the camera selection is updated accordingly

#### Scenario: Manual adjustment overrides preset
- **WHEN** user has applied a preset (preset is active/highlighted)
- **AND** user manually changes any setting (layout, quality, bitrate)
- **THEN** the preset becomes inactive (no longer highlighted)
- **AND** the manual changes take effect
- **AND** the preset itself is NOT modified

### Requirement: Create new preset

The system SHALL allow users to create a new preset from the current video wall configuration.

#### Scenario: Create preset with valid name
- **WHEN** user clicks the "+" button
- **AND** enters a unique preset name
- **AND** clicks confirm
- **THEN** a new preset is created with the current settings
- **AND** the preset appears in the list
- **AND** the user receives a success message

#### Scenario: Create preset with duplicate name
- **WHEN** user enters a preset name that already exists
- **AND** clicks confirm
- **THEN** an error message is displayed
- **AND** the preset is not created

#### Scenario: Create preset with empty name
- **WHEN** user clicks confirm without entering a name
- **THEN** a validation message is displayed
- **AND** the preset is not created

### Requirement: Edit preset

The system SHALL allow users to edit both the name and configuration of an existing preset.

#### Scenario: Edit preset name
- **WHEN** user clicks the edit icon on a preset
- **AND** enters a new unique name
- **AND** confirms the edit
- **THEN** the preset name is updated
- **AND** the change is persisted

#### Scenario: Edit preset configuration
- **WHEN** user clicks the edit icon on a preset
- **AND** modifies the preset settings (layout, quality, bitrate, cameraIds)
- **AND** confirms the edit
- **THEN** the preset configuration is updated
- **AND** the change is persisted

#### Scenario: Edit built-in preset
- **WHEN** user attempts to edit a built-in preset
- **THEN** the edit option is disabled or hidden
- **AND** a tooltip explains that system presets cannot be edited

### Requirement: Delete preset

The system SHALL allow users to delete a preset, with confirmation required before deletion.

#### Scenario: Delete user preset with confirmation
- **WHEN** user clicks the delete icon on a user-created preset
- **AND** confirms the deletion in the popup
- **THEN** the preset is removed from the list
- **AND** the user receives a success message

#### Scenario: Cancel preset deletion
- **WHEN** user clicks the delete icon on a preset
- **AND** cancels the deletion
- **THEN** the preset remains in the list unchanged

### Requirement: Default preset

The system SHALL support marking one preset as the default, which will be considered when the video wall page loads.

#### Scenario: Set default preset
- **WHEN** user right-clicks or uses a context menu on a preset
- **AND** selects "Set as default"
- **THEN** the preset is marked as the default
- **AND** previously default preset (if any) loses its default status

#### Scenario: Built-in default preset
- **WHEN** the video wall page loads
- **AND** the user has no preferences configured
- **THEN** the built-in "四分屏" preset is shown as the default

### Requirement: Drag-to-reorder presets

The system SHALL allow users to drag and reorder their presets in the list.

#### Scenario: Drag preset to new position
- **WHEN** user drags a preset item to a new position in the list
- **THEN** the preset order is updated visually
- **AND** the new order is persisted to the database

#### Scenario: Reorder built-in presets
- **WHEN** user attempts to drag a built-in preset
- **THEN** the drag is disabled
- **AND** built-in presets maintain their fixed position

### Requirement: Preset name uniqueness

The system SHALL enforce unique preset names per user.

#### Scenario: Duplicate preset name
- **WHEN** user attempts to create or rename a preset with a name that already exists
- **THEN** an error message is displayed
- **AND** the preset creation/rename is rejected
