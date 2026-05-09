# Animation System

## ADDED Requirements

### Requirement: Pulse Glow Animation

The system SHALL provide a pulse glow animation effect for online status indicators to create a breathing/heartbeat visual effect.

#### Scenario: Pulse glow effect

- **WHEN** a status indicator has the `status-online` class
- **THEN** the indicator SHALL have a pulsing glow animation
- **AND** the animation SHALL cycle every 2 seconds
- **AND** the glow color SHALL match the `--status-online` variable

### Requirement: Count-Up Animation

The system SHALL provide a count-up animation for statistics on the Dashboard page where numbers animate from 0 to their final value.

#### Scenario: Number count-up

- **WHEN** a statistic card loads
- **THEN** the number SHALL animate from 0 to its final value over 0.6 seconds
- **AND** the animation SHALL use ease-out timing function

### Requirement: Fade-In Animation

The system SHALL provide a fade-in animation for page content transitions.

#### Scenario: Content fade-in

- **WHEN** a new page or content loads
- **THEN** the content SHALL fade in with 0.3 seconds duration
- **AND** the opacity SHALL transition from 0 to 1

### Requirement: Slide-In Animation

The system SHALL provide a slide-in animation for sidebar navigation items.

#### Scenario: Sidebar item slide-in

- **WHEN** the sidebar loads or an item is highlighted
- **THEN** the item SHALL slide in from the left with 0.2 seconds duration

### Requirement: Reduced Motion Support

The system SHALL respect the user's `prefers-reduced-motion` preference and disable or reduce animations for users who prefer reduced motion.

#### Scenario: Reduced motion preference

- **WHEN** the user has `prefers-reduced-motion: reduce` set
- **THEN** all animations SHALL be disabled or reduced to simple opacity transitions
