# card-spacing-responsive

## ADDED Requirements

### Requirement: Responsive gutter adjustment

Row components SHALL provide responsive gutter values for different screen sizes.

| Breakpoint | Tailwind | Gutter Value |
|------------|----------|--------------|
| Mobile (< 640px) | `xs` | `[8, 8]` |
| Tablet (640-768px) | `sm` | `[12, 12]` |
| Desktop (> 768px) | `lg`, `xl` | `[16, 16]` |

#### Scenario: Mobile card grid
- **WHEN** rendering a card grid on mobile devices
- **THEN** the Row gutter SHALL use `{ xs: 8, sm: 12, lg: 16 }` for both horizontal and vertical spacing

#### Scenario: Desktop card grid
- **WHEN** rendering a card grid on desktop devices
- **THEN** the Row gutter SHALL use `gutter={[16, 16]}` (fixed value)

### Requirement: Responsive block spacing

Page sections SHALL adjust vertical spacing based on screen size.

#### Scenario: Mobile section separation
- **WHEN** rendering primary sections on mobile
- **THEN** the section SHALL use `className="mb-4"` (reduced from desktop's `mb-6`)

#### Scenario: Mobile component internal spacing
- **WHEN** rendering internal component spacing on mobile
- **THEN** spacing SHALL be reduced proportionally: `mt-2` instead of `mt-3`

### Requirement: Mobile-specific card padding

Card padding MAY be reduced on mobile for space efficiency.

#### Scenario: Mobile stat card
- **WHEN** rendering a stat card on mobile (< 640px)
- **THEN** padding MAY be reduced to 16px (from desktop's 20px) via responsive class

#### Scenario: Mobile compact card
- **WHEN** rendering a compact card on mobile
- **THEN** padding SHALL remain 12px (no change needed)
