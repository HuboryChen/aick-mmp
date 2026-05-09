# card-layout-tokens

## ADDED Requirements

### Requirement: Gutter spacing token

All Row components displaying cards SHALL use `gutter={[16, 16]}` to define horizontal and vertical spacing between cards.

#### Scenario: Standard page card grid
- **WHEN** rendering a grid of cards on a standard page (Dashboard, Analytics, etc.)
- **THEN** the Row component SHALL use `gutter={[16, 16]}`

#### Scenario: Modal form layout
- **WHEN** rendering form fields inside a Modal
- **THEN** the Row component SHALL use `gutter={[16, 16]}`

### Requirement: Block spacing token

Page sections containing cards SHALL use Tailwind spacing classes for vertical separation.

| Section Level | Tailwind Class | Pixel Value |
|---------------|----------------|-------------|
| Primary blocks | `mb-6` | 24px |
| Secondary blocks | `mb-4` | 16px |
| Tertiary elements | `mt-2` / `mb-2` | 8px |
| Within components | `mt-3` / `mb-1` | 12px / 4px |

#### Scenario: Primary section separation
- **WHEN** separating major page sections (e.g., stats row from chart row)
- **THEN** the section container SHALL use `className="mb-6"`

#### Scenario: Card row internal spacing
- **WHEN** separating rows within a card section
- **THEN** the inner Row SHALL use `gutter={[16, 16]}` to control card spacing

### Requirement: Card padding token

Card components SHALL use consistent padding based on their type.

| Card Type | Padding | Use Cases |
|-----------|---------|-----------|
| Stat card | 20px | Dashboard KPIs, Analytics statistics |
| Normal card | 16px | Regional stats, list containers, detail panels |
| Compact card | 12px | Filter controls, history lists, Modal contents |

#### Scenario: Stat card rendering
- **WHEN** rendering a stat card (IndustrialStatCard, StatsCard)
- **THEN** the Card body padding SHALL be 20px: `styles={{ body: { padding: 20 } }}`

#### Scenario: Normal card rendering
- **WHEN** rendering a standard card (IndustrialCard, region cards)
- **THEN** the Card body padding SHALL be 16px: `styles={{ body: { padding: 16 } }}`

#### Scenario: Compact card rendering
- **WHEN** rendering a compact card (filter controls, history items)
- **THEN** the Card body padding SHALL be 12px: `styles={{ body: { padding: 12 } }}`

### Requirement: No inline spacing

Inline styles for margin or padding SHALL NOT be used in card layouts.

#### Scenario: Converting legacy inline styles
- **WHEN** encountering `style={{ marginBottom: 16 }}` on card containers
- **THEN** it SHALL be converted to `className="mb-4"` (Tailwind equivalent)

#### Scenario: New card implementation
- **WHEN** implementing new card layouts
- **THEN** spacing SHALL be specified using Tailwind classes only
