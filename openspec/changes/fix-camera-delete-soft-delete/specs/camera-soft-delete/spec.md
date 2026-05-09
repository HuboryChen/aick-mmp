## ADDED Requirements

### Requirement: Camera soft delete sets both fields

The system SHALL set both `isDeleted=true` and `deletedAt` timestamp when a camera is deleted via the API.

#### Scenario: Single camera deletion
- **WHEN** admin sends DELETE request to `/cameras/{id}`
- **THEN** the camera's `isDeleted` field SHALL be set to `true`
- **AND** the camera's `deletedAt` field SHALL be set to current timestamp
- **AND** both fields SHALL be persisted to the database within the same transaction

#### Scenario: Batch camera deletion
- **WHEN** admin sends batch delete request to `/cameras/batch-operation` with `operation=DELETE`
- **THEN** all cameras in the request SHALL have both `isDeleted=true` and `deletedAt` set

### Requirement: Deleted cameras are excluded from list queries

The system SHALL exclude soft-deleted cameras from all list queries that return active cameras.

#### Scenario: Get all cameras returns only active
- **WHEN** user requests GET `/cameras`
- **THEN** the response SHALL NOT include cameras where `deletedAt IS NOT NULL`

#### Scenario: Get cameras by region excludes deleted
- **WHEN** user requests GET `/cameras?regionId={id}`
- **THEN** the response SHALL NOT include cameras with `deletedAt IS NOT NULL` for the specified region

#### Scenario: Get cameras by edge node excludes deleted
- **WHEN** user requests GET `/cameras?edgeNodeId={id}`
- **THEN** the response SHALL NOT include cameras with `deletedAt IS NOT NULL` for the specified edge node

### Requirement: Batch delete API endpoint

The system SHALL expose a batch delete endpoint that matches the frontend API path.

#### Scenario: Successful batch delete
- **WHEN** admin sends POST request to `/cameras/batch-operation` with body `{"operation":"DELETE","cameraIds":[1,2,3]}`
- **THEN** the system SHALL delete all cameras with the specified IDs
- **AND** return 200 OK with `{"processedIds":[1,2,3],"failedIds":[]}`

#### Scenario: Batch delete with partial failure
- **WHEN** admin sends POST request to `/cameras/batch-operation` with some invalid IDs
- **THEN** the system SHALL attempt to delete all valid IDs
- **AND** return 200 OK with both `processedIds` and `failedIds` populated
