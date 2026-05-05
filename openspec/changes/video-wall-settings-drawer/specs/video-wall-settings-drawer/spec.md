# Video Wall Settings Drawer

## ADDED Requirements

### Requirement: Drawer trigger button

The system SHALL provide a settings button in the video wall header that opens the settings drawer when clicked.

#### Scenario: Open drawer via button click
- **WHEN** user clicks the settings icon in the video wall header
- **THEN** the settings drawer slides in from the right side of the screen
- **AND** the button shows an active state indicator

#### Scenario: Close drawer via close button
- **WHEN** user clicks the close button (X) in the drawer header
- **THEN** the drawer slides out to the right
- **AND** the current settings state is preserved

#### Scenario: Close drawer via mask click
- **WHEN** user clicks on the overlay mask outside the drawer
- **THEN** the drawer closes
- **AND** the current settings state is preserved

#### Scenario: Close drawer via ESC key
- **WHEN** user presses the ESC key while drawer is open
- **THEN** the drawer closes
- **AND** the current settings state is preserved

### Requirement: Drawer layout structure

The settings drawer SHALL contain the following sections in order: preset selector, layout selector, and quality selector, separated by dividers.

#### Scenario: Drawer displays sections in correct order
- **WHEN** the drawer is opened
- **THEN** the preset selector appears at the top
- **AND** a divider separates it from the layout selector
- **AND** another divider separates layout selector from quality selector

### Requirement: Real-time configuration application

The system SHALL apply configuration changes to the video wall in real-time as the user makes selections, without requiring a confirmation step.

#### Scenario: Layout change applies immediately
- **WHEN** user selects a different layout option
- **THEN** the video wall grid updates immediately to reflect the new layout

#### Scenario: Quality change applies immediately
- **WHEN** user selects a different quality option
- **THEN** the video streams update immediately to use the new quality setting

#### Scenario: Bitrate slider change applies immediately
- **WHEN** user adjusts the bitrate slider
- **THEN** the video streams update immediately to use the new bitrate

### Requirement: Drawer footer actions

The drawer SHALL have a footer with "Reset" and "Done" buttons.

#### Scenario: Reset restores default settings
- **WHEN** user clicks the "Reset" button
- **THEN** all settings revert to default values (layout: 4, quality: 720p, bitrate: 2048)
- **AND** the video wall updates to reflect the default settings

#### Scenario: Done button closes drawer
- **WHEN** user clicks the "Done" button
- **THEN** the drawer closes
- **AND** current settings are persisted

### Requirement: Drawer width and positioning

The drawer SHALL be positioned on the right side of the screen with a width of 320px.

#### Scenario: Drawer appears on right side
- **WHEN** the drawer is opened
- **THEN** it appears on the right side of the screen
- **AND** it has a width of 320 pixels
