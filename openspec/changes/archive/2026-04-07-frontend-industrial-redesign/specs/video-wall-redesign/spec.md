# Video Wall Redesign

## ADDED Requirements

### Requirement: Immersive Dark Background

The video wall page SHALL have an immersive dark background that provides maximum contrast for video content.

#### Scenario: Video wall background

- **WHEN** the user navigates to the video wall page
- **THEN** the background SHALL use `--color-bg-primary` color
- **AND** the background SHALL be solid (no gradients or patterns)

### Requirement: Video Card Design

Each video window SHALL have a consistent industrial-style card design with hover effects and status indicators.

#### Scenario: Video card hover effect

- **WHEN** the user hovers over a video card
- **THEN** a subtle border glow SHALL appear using the `--color-accent` variable
- **AND** control buttons SHALL fade in

#### Scenario: Video card status indicator

- **WHEN** a video stream is active
- **THEN** a status dot SHALL be displayed with `status-online` color
- **AND** the status dot SHALL have pulse glow animation

### Requirement: Fullscreen Mode

The video wall SHALL support a fullscreen mode that hides UI chrome and maximizes video viewing area.

#### Scenario: Enter fullscreen

- **WHEN** the user clicks the fullscreen button
- **THEN** the video wall SHALL expand to fill the entire viewport
- **AND** the browser fullscreen API SHALL be activated
- **AND** all navigation UI SHALL be hidden

#### Scenario: Exit fullscreen

- **WHEN** the user presses Escape or clicks the exit fullscreen button
- **THEN** the video wall SHALL return to normal mode
- **AND** all navigation UI SHALL be restored

### Requirement: Layout Grid Switching

The video wall SHALL support multiple grid layouts (1x1, 2x2, 3x3, 4x4) with smooth transition animations.

#### Scenario: Grid layout change

- **WHEN** the user selects a different grid layout
- **THEN** the video windows SHALL reorganize smoothly
- **AND** the transition SHALL complete within 0.3 seconds
