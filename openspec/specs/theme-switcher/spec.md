# Theme Switcher

## ADDED Requirements

### Requirement: Theme State Persistence

The system SHALL persist the user's theme preference in localStorage so that the preference survives page reloads and browser restarts.

#### Scenario: Save theme preference

- **WHEN** the user toggles the theme
- **THEN** the theme value SHALL be saved to localStorage under the key `theme`

#### Scenario: Restore theme preference

- **WHEN** the application loads
- **THEN** the system SHALL read the theme value from localStorage
- **AND** if the value exists, SHALL apply that theme
- **AND** if no value exists, SHALL apply dark theme as default

### Requirement: Theme Toggle Component

The system SHALL provide a theme toggle component in the Header that allows users to switch between dark and light modes.

#### Scenario: Toggle button present

- **WHEN** the user is authenticated
- **THEN** the Header SHALL display a theme toggle button

#### Scenario: Toggle changes theme

- **WHEN** the user clicks the theme toggle button
- **THEN** the theme SHALL switch from dark to light or light to dark
- **AND** the CSS `data-theme` attribute on `<html>` SHALL be updated

### Requirement: Theme Context Provider

The system SHALL provide a React Context that manages the current theme state and provides theme-related utilities to child components.

#### Scenario: Context provides theme value

- **WHEN** a component is wrapped by ThemeProvider
- **THEN** the component SHALL have access to `theme` (current theme name)
- **AND** the component SHALL have access to `toggleTheme` (function to switch theme)

### Requirement: Ant Design ConfigProvider Integration

The system SHALL synchronize the theme state with Ant Design's ConfigProvider to ensure all Ant Design components respect the current theme.

#### Scenario: ConfigProvider updates on theme change

- **WHEN** the theme changes
- **THEN** the Ant Design ConfigProvider SHALL receive updated Design Tokens
- **AND** all Ant Design components SHALL reflect the new theme
