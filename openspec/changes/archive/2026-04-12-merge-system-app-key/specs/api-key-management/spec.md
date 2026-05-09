# api-key-management

## ADDED Requirements

### Requirement: User API key creation

The system SHALL allow users to create API keys for personal use.

#### Scenario: Create user API key
- **WHEN** user creates an API key with name
- **THEN** system generates unique `access_key` (prefixed with `ak_`)
- **AND** system generates `secret_key` (prefixed with `sk_`)
- **AND** system encrypts and stores `secret_key`
- **AND** system returns `access_key`, `secret_key`, name, created_at

### Requirement: User API key listing

The system SHALL allow users to list their own API keys.

#### Scenario: List user API keys
- **WHEN** user requests their API key list
- **THEN** system returns all keys belonging to the user
- **AND** system excludes `secret_key` from response (security)
- **AND** system includes id, access_key, name, status, created_at, last_used_at

### Requirement: User API key status update

The system SHALL allow users to enable or disable their own API keys.

#### Scenario: Disable API key
- **WHEN** user disables their own API key
- **THEN** system sets key status to DISABLED
- **AND** system rejects authentication attempts using this key

#### Scenario: Enable API key
- **WHEN** user enables their own API key
- **THEN** system sets key status to ENABLED
- **AND** system accepts authentication attempts using this key

### Requirement: User API key deletion

The system SHALL allow users to delete their own API keys.

#### Scenario: Delete own API key
- **WHEN** user deletes their own API key
- **THEN** system removes key record from database
- **AND** system invalidates any cached credentials

#### Scenario: Delete another user's API key
- **WHEN** user attempts to delete another user's API key
- **THEN** system rejects the request
- **AND** system returns authorization error

### Requirement: User API key authentication

The system SHALL authenticate users using API key signature verification.

#### Scenario: Successful authentication
- **WHEN** user provides valid `access_key`, signature, timestamp, method, path
- **THEN** system verifies signature matches
- **THEN** system updates `last_used_at` timestamp
- **THEN** system grants access

#### Scenario: Invalid API key
- **WHEN** user provides non-existent or invalid `access_key`
- **THEN** system rejects authentication
- **AND** system returns invalid key error

## REMOVED Requirements

### Requirement: System-level API key management

**Reason**: System-level keys are now integrated into SystemApp entity for simplified management.

**Migration**: Use SystemApp CRUD endpoints at `/system-apps` for system credential management.
