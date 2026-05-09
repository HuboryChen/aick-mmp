# System App Management (system-app-management)

## ADDED Requirements

### Requirement: System app creation
The system SHALL allow administrators to create system applications.

#### Scenario: Admin creates system app
- **WHEN** admin POSTs to `/system-apps` with:
  ```json
  {
    "name": "Edge Cluster A",
    "description": "Edge nodes in Region A",
    "permissions": ["EDGE_REGISTER", "EDGE_HEARTBEAT"]
  }
  ```
- **THEN** system SHALL generate a unique `app_key` (UUID format)
- **AND** system SHALL create the system app record with status ACTIVE
- **AND** system SHALL associate it with the admin user as owner

#### Scenario: System app creation with user owner
- **WHEN** admin POSTs to `/system-apps` with `ownerId` set to a user ID
- **THEN** system SHALL create the app with ownerType USER
- **AND** system SHALL associate the app with the specified user

---

### Requirement: System app listing
The system SHALL allow administrators to list all system applications.

#### Scenario: Admin lists system apps
- **WHEN** admin GETs `/system-apps`
- **THEN** system SHALL return paginated list of all system apps
- **AND** each app SHALL include id, app_key, name, status, permission count

#### Scenario: Admin filters system apps by status
- **WHEN** admin GETs `/system-apps?status=ACTIVE`
- **THEN** system SHALL return only system apps with ACTIVE status

---

### Requirement: System app details
The system SHALL allow administrators to view system app details.

#### Scenario: Admin views system app
- **WHEN** admin GETs `/system-apps/{id}`
- **THEN** system SHALL return full system app details
- **AND** details SHALL include all associated API key counts

---

### Requirement: System app update
The system SHALL allow administrators to update system app properties.

#### Scenario: Admin updates app name and description
- **WHEN** admin PUTs `/system-apps/{id}` with updated `name` and `description`
- **THEN** system SHALL update the specified fields
- **AND** system SHALL preserve other existing values

#### Scenario: Admin updates app status
- **WHEN** admin PUTs `/system-apps/{id}` with `status`: `INACTIVE`
- **THEN** system SHALL update the status
- **AND** system SHALL disable all associated API keys
- **AND** all requests using affected keys SHALL be rejected

#### Scenario: Admin updates app permissions
- **WHEN** admin PUTs `/system-apps/{id}` with updated `permissions`
- **THEN** system SHALL replace the existing permission set
- **AND** Edge nodes using this app SHALL have updated permissions on next heartbeat

---

### Requirement: System app deletion
The system SHALL allow administrators to delete system applications.

#### Scenario: Admin deletes system app with no keys
- **WHEN** admin DELETEs `/system-apps/{id}`
- **AND** the app has no associated API keys
- **THEN** system SHALL permanently remove the system app record

#### Scenario: Admin deletes system app with keys
- **WHEN** admin DELETEs `/system-apps/{id}`
- **AND** the app has associated API keys
- **THEN** system SHALL return 400 Bad Request
- **AND** system SHALL indicate keys must be deleted first

#### Scenario: Admin deletes system app with Edge nodes
- **WHEN** admin DELETEs `/system-apps/{id}`
- **AND** the app has associated Edge nodes
- **THEN** system SHALL return 400 Bad Request
- **AND** system SHALL indicate Edge nodes must be disassociated first

---

### Requirement: Predefined system app permissions
The system SHALL support predefined permission sets for system applications.

#### Scenario: Available permissions
The system SHALL support the following predefined permissions:
- `EDGE_REGISTER` - Allows Edge node registration
- `EDGE_HEARTBEAT` - Allows heartbeat reporting
- `EDGE_CONFIG_UPDATE` - Allows receiving configuration updates

#### Scenario: Custom permission rejection
- **WHEN** admin creates app with undefined permission
- **THEN** system SHALL return 400 Bad Request
- **AND** system SHALL list available permissions
