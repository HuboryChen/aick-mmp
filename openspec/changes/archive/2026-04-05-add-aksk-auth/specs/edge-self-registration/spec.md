# Edge Self-Registration (edge-self-registration)

## ADDED Requirements

### Requirement: Edge node self-registration
The system SHALL allow Edge nodes to self-register using AK/SK credentials.

#### Scenario: Edge node registers successfully
- **WHEN** Edge node POSTs to `/edge/register` with:
  - `X-Access-Key` header with valid system-level AK
  - `X-Signature` header with valid signature
  - `X-Timestamp` header within tolerance
  - Body containing node information:
    ```json
    {
      "name": "edge-node-01",
      "location": "Region A - Building 1",
      "ipAddress": "192.168.1.100",
      "port": 8081,
      "maxCameraSupport": 8
    }
    ```
- **THEN** system SHALL validate the AK/SK credentials
- **AND** system SHALL verify the associated SystemApp has EDGE_REGISTER permission
- **AND** system SHALL create an EdgeNode record with status ONLINE
- **AND** system SHALL associate the EdgeNode with the SystemApp
- **AND** system SHALL return the registered node details including generated UUID

#### Scenario: Edge registration with duplicate name
- **WHEN** Edge node attempts registration with a name that already exists
- **THEN** system SHALL return 400 Bad Request
- **AND** system SHALL indicate the name is already taken

#### Scenario: Edge registration with invalid AK
- **WHEN** Edge node POSTs to `/edge/register` with invalid access key
- **THEN** system SHALL return 401 Unauthorized

#### Scenario: Edge registration without EDGE_REGISTER permission
- **WHEN** Edge node POSTs to `/edge/register` with valid AK/SK
- **AND** the associated SystemApp does not have EDGE_REGISTER permission
- **THEN** system SHALL return 403 Forbidden

---

### Requirement: Edge node heartbeat
The system SHALL accept heartbeat reports from registered Edge nodes.

#### Scenario: Valid heartbeat from registered Edge
- **WHEN** registered Edge node POSTs to `/edge-nodes/{id}/heartbeat` with:
  - `X-Access-Key` header with valid system-level AK
  - `X-Signature` header with valid signature
  - Body containing metrics:
    ```json
    {
      "cpuUsage": 45.5,
      "memoryUsage": 62.3,
      "storageUsage": 30.0,
      "currentCameraCount": 4
    }
    ```
- **THEN** system SHALL validate the Edge node exists and is associated with the AK
- **AND** system SHALL update the EdgeNode record with latest heartbeat time
- **AND** system SHALL update resource metrics

#### Scenario: Heartbeat from unregistered Edge
- **WHEN** Edge node sends heartbeat without prior registration
- **THEN** system SHALL return 404 Not Found

#### Scenario: Heartbeat from disabled Edge
- **WHEN** Edge node sends heartbeat but EdgeNode status is not ONLINE
- **THEN** system SHALL return 400 Bad Request

---

### Requirement: Edge node authentication via AK/SK
The system SHALL use AK/SK for Edge node authentication instead of username/password.

#### Scenario: Remove authUsername/authPassword from EdgeNode
- **WHEN** EdgeNode record is created or updated
- **THEN** system SHALL NOT require or store authUsername
- **AND** system SHALL NOT require or store authPassword

#### Scenario: Edge node associates with SystemApp
- **WHEN** Edge node is registered
- **THEN** system SHALL store the app_id reference to SystemApp
- **AND** all Edge node authentication SHALL validate via the associated SystemApp's AK/SK

---

### Requirement: Edge node status management
The system SHALL manage Edge node status based on heartbeat activity.

#### Scenario: Edge goes offline after heartbeat timeout
- **WHEN** Edge node has not sent heartbeat for configured timeout (e.g., 60 seconds)
- **THEN** system SHALL update EdgeNode status to OFFLINE
- **AND** system SHALL log the status change

#### Scenario: Edge comes back online
- **WHEN** Edge node sends heartbeat after being OFFLINE
- **THEN** system SHALL update EdgeNode status to ONLINE
- **AND** system SHALL log the status change

---

### Requirement: Simplified signature for Edge operations
The system SHALL use simplified signature verification for Edge node operations.

#### Scenario: Full signature for registration
- **WHEN** Edge node registers
- **THEN** system SHALL verify full signature including timestamp
- **AND** system SHALL reject requests with timestamps outside ±5 minutes

#### Scenario: Simplified verification for heartbeat
- **WHEN** Edge node sends heartbeat
- **THEN** system SHALL verify access key validity
- **AND** system SHALL verify timestamp is within tolerance
- **AND** system SHALL NOT require full signature verification (for performance)

---

### Requirement: Edge node registration removes auth fields
The system SHALL remove authUsername and authPassword fields from the EdgeNode entity.

#### Scenario: EdgeNode entity update
The EdgeNode entity SHALL:
- **REMOVE** `authUsername` field
- **REMOVE** `authPassword` field
- **ADD** `appId` field (FK to SystemApp)
- **ADD** `registeredAt` field (registration timestamp)

**Reason**: Authentication now handled via AK/SK at the SystemApp level

**Migration**: Existing EdgeNode records with auth credentials should be migrated by:
1. Creating corresponding SystemApp records
2. Creating AK/SK for each SystemApp
3. Updating EdgeNode records to reference SystemApp
4. Clearing authUsername and authPassword fields
