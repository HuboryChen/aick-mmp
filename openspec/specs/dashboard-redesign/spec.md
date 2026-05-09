# Dashboard Redesign

## ADDED Requirements

### Requirement: Glassmorphism Card Design

Dashboard statistics cards SHALL use a glassmorphism (frosted glass) design effect with subtle transparency and blur.

#### Scenario: Glassmorphism card appearance

- **WHEN** a statistic card is rendered
- **THEN** the card SHALL have a semi-transparent background
- **AND** the card SHALL have a backdrop-filter blur effect
- **AND** the card SHALL have a subtle border using `--color-accent` at low opacity

### Requirement: Animated Statistics

Dashboard statistics SHALL display count-up animations when values change or on initial load.

#### Scenario: Number animation on load

- **WHEN** the Dashboard page loads
- **THEN** each statistic value SHALL animate from 0 to its actual value
- **AND** the animation SHALL complete within 0.6 seconds
- **AND** the animation SHALL use an ease-out timing function

### Requirement: Progress Bar Styling

Statistics with progress indicators SHALL have industrial-style progress bars with gradient fills and glow effects.

#### Scenario: Progress bar appearance

- **WHEN** a progress bar is rendered
- **THEN** the fill SHALL use a gradient from `--color-accent` to a lighter variant
- **AND** the progress bar SHALL have a subtle glow effect when animating
- **AND** the inactive portion SHALL use `--color-bg-secondary`

### Requirement: Status Indicators

Dashboard SHALL display status indicators with appropriate colors and pulse animations for online/offline states.

#### Scenario: Online status indicator

- **WHEN** an entity (camera, edge node) is online
- **THEN** the status indicator SHALL use `--status-online` color
- **AND** the indicator SHALL have a pulse glow animation

#### Scenario: Offline status indicator

- **WHEN** an entity (camera, edge node) is offline
- **THEN** the status indicator SHALL use `--status-offline` color
- **AND** the indicator SHALL NOT have pulse animation

### Requirement: Alert List Styling

Alert items in the Dashboard SHALL be styled consistently with industrial theme colors and appropriate icons.

#### Scenario: Error alert styling

- **WHEN** an error alert is displayed
- **THEN** the alert SHALL use `--status-offline` color
- **AND** the alert SHALL display an exclamation icon

#### Scenario: Warning alert styling

- **WHEN** a warning alert is displayed
- **THEN** the alert SHALL use `--status-warning` color
- **AND** the alert SHALL display a warning icon
