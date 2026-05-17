## MODIFIED Requirements

### Requirement: PRD color specification alignment

The PRD color specification (section 6.2.1) SHALL document the industrial dark theme as the primary color system, consistent with the implemented CLAUDE.md theme. The light theme SHALL be documented as an optional alternative.

**Change**: PRD 6.2.1 section updated to reflect the implemented dark theme (#00d4ff accent, #0a0e17 background, glassmorphism cards) instead of the generic Ant Design blue theme (#1890ff, #f0f2f5).

#### Scenario: PRD documents dark theme

- **WHEN** a developer reads the PRD color specification
- **THEN** the primary color SHALL be #00d4ff (not #1890ff)
- **AND** the background color SHALL be the dark theme colors from industrial-theme spec
- **AND** the light theme SHALL be documented as an optional alternative

#### Scenario: Design tokens match implementation

- **WHEN** the PRD references color values
- **THEN** they SHALL match the CSS variables defined in CLAUDE.md and theme.css
- **AND** any discrepancy SHALL be flagged as a documentation bug
