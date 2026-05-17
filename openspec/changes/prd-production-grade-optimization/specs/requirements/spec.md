## MODIFIED Requirements

### Requirement: User password policy

The default password SHALL NOT be hardcoded in documentation or code. New user accounts SHALL be assigned a system-generated random password. Users SHALL be required to change their password on first login.

**Change**: Previously documented default password "admin/admin123" is removed. First-login password change is now mandatory.

#### Scenario: New user first login

- **WHEN** a new user account is created
- **THEN** a system-generated random password SHALL be sent via secure channel
- **AND** the user SHALL be required to set a new password upon first login

#### Scenario: Force password change

- **WHEN** a user logs in for the first time with the system-generated password
- **THEN** the system SHALL redirect to the password change page
- **AND** block access to other features until password is changed

### Requirement: Target market scope

The MVP version SHALL target two primary market segments: chain retail and warehouse/logistics enterprises with 50+ locations.

**Change**: Scope narrowed from 5 segments (retail, industrial, smart city, construction, logistics) to 2 segments for MVP.

#### Scenario: MVP focus validated

- **WHEN** the MVP is released
- **THEN** it SHALL target chain retail and warehouse/logistics markets
- **AND** the product demo SHALL be tailored to multi-site unified management use cases

## ADDED Requirements

### Requirement: Pricing section ordering fix

The "商业模式与定价" section SHALL appear after "风险评估" section (section 11), not before it.

#### Scenario: Document section ordering

- **WHEN** a developer reads the PRD
- **THEN** the section numbering SHALL be sequential without ordering errors
