# system-app-with-key

## ADDED Requirements

### Requirement: System app creation with integrated key generation

The system SHALL allow administrators to create system applications with automatically generated credentials in a single operation.

#### Scenario: Successful system app creation
- **WHEN** admin creates a system app with required fields (name, description, permissions)
- **THEN** system generates unique `app_key` (prefixed with `ak_`)
- **AND** system generates `app_secret` (prefixed with `sk_`)
- **AND** system encrypts and stores `app_secret`
- **AND** system returns `app_key`, `app_secret`, name, created_at

#### Scenario: System app creation with optional fields
- **WHEN** admin creates a system app with optional fields (expires_at)
- **THEN** system validates expires_at is in the future
- **AND** system includes expires_at in the response

### Requirement: System app credential retrieval

The system SHALL allow administrators to retrieve system app credentials (only for newly created apps that haven't been retrieved).

#### Scenario: Retrieve credentials for new app
- **WHEN** admin retrieves credentials within 1 hour of creation
- **THEN** system returns decrypted `app_secret`
- **AND** system marks credentials as retrieved

#### Scenario: Retrieve credentials after timeout
- **WHEN** admin retrieves credentials after 1 hour
- **THEN** system returns only `app_key`
- **AND** system indicates `app_secret` was already retrieved

### Requirement: System app key regeneration

The system SHALL allow administrators to regenerate system app credentials.

#### Scenario: Regenerate credentials
- **WHEN** admin requests credential regeneration for an active system app
- **THEN** system generates new `app_key` and `app_secret`
- **AND** system invalidates old credentials immediately
- **AND** system returns new credentials

### Requirement: System app status management

The system SHALL allow administrators to manage system app status (ACTIVE/INACTIVE/SUSPENDED).

#### Scenario: Suspend system app
- **WHEN** admin sets system app status to SUSPENDED
- **THEN** system rejects authentication attempts using this app's credentials
- **AND** system records suspension timestamp

#### Scenario: Reactivate system app
- **WHEN** admin sets system app status to ACTIVE
- **THEN** system accepts authentication attempts using this app's credentials

### Requirement: System app deletion

The system SHALL allow administrators to delete system applications.

#### Scenario: Delete active system app
- **WHEN** admin deletes a system app
- **THEN** system marks all associated credentials as invalid
- **AND** system removes app record from database
- **AND** system rejects any authentication attempts

### Requirement: System app authentication

The system SHALL authenticate system applications using AK/SK signature verification.

#### Scenario: Successful authentication
- **WHEN** system app provides valid `app_key`, signature, timestamp, method, path
- **THEN** system verifies signature matches
- **THEN** system updates `last_used_at` timestamp
- **THEN** system grants access

#### Scenario: Invalid signature
- **WHEN** system app provides invalid signature
- **THEN** system rejects authentication
- **AND** system logs authentication failure

#### Scenario: Expired credentials
- **WHEN** system app provides credentials with past `expires_at`
- **THEN** system rejects authentication
- **AND** system returns credential expiration error

#### Scenario: Suspended app authentication
- **WHEN** system app provides valid credentials but status is SUSPENDED
- **THEN** system rejects authentication
- **AND** system returns app suspended error
