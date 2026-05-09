# Industrial Theme System

## ADDED Requirements

### Requirement: CSS Variables Theme System

The system SHALL provide a CSS Variables-based theme system that enables consistent styling across all components with two color modes: dark (default) and light.

#### Scenario: Dark mode default

- **WHEN** the application loads for the first time
- **THEN** the dark theme SHALL be applied as the default

#### Scenario: CSS variables available

- **WHEN** the theme is set to dark mode
- **THEN** CSS variable `--color-bg-primary` SHALL be `#0a0e17`
- **AND** CSS variable `--color-bg-secondary` SHALL be `#141820`
- **AND** CSS variable `--color-bg-card` SHALL be `#1a1f2e`
- **AND** CSS variable `--color-accent` SHALL be `#00d4ff`
- **AND** CSS variable `--color-text-primary` SHALL be `#ffffff`
- **AND** CSS variable `--color-text-secondary` SHALL be `#94a3b8`
- **AND** CSS variable `--status-online` SHALL be `#00ff88`
- **AND** CSS variable `--status-offline` SHALL be `#ff4757`
- **AND** CSS variable `--status-warning` SHALL be `#fbbf24`

### Requirement: Light Mode Color Scheme

The system SHALL provide a light mode color scheme with improved readability for bright environments.

#### Scenario: Light mode variables

- **WHEN** the theme is set to light mode
- **THEN** CSS variable `--color-bg-primary` SHALL be `#f1f5f9`
- **AND** CSS variable `--color-bg-secondary` SHALL be `#ffffff`
- **AND** CSS variable `--color-bg-card` SHALL be `#ffffff`
- **AND** CSS variable `--color-accent` SHALL be `#0284c7`
- **AND** CSS variable `--color-text-primary` SHALL be `#0f172a`
- **AND** CSS variable `--color-text-secondary` SHALL be `#64748b`
- **AND** CSS variable `--status-online` SHALL be `#16a34a`
- **AND** CSS variable `--status-offline` SHALL be `#dc2626`
- **AND** CSS variable `--status-warning` SHALL be `#d97706`

### Requirement: Tailwind CSS Integration

The system SHALL integrate Tailwind CSS with the theme system to enable utility-first styling.

#### Scenario: Tailwind configured

- **WHEN** Tailwind CSS is configured
- **THEN** Tailwind SHALL be configured to use CSS variables for all color references
- **AND** Tailwind `dark:` variant SHALL be enabled
- **AND** PostCSS autoprefixer SHALL be configured
