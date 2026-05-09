# API Key Authentication (api-key-auth)

## ADDED Requirements

### Requirement: AK/SK creation for users
The system SHALL allow authenticated users to create personal API keys (AK/SK) for programmatic access.

#### Scenario: User creates AK/SK
- **WHEN** authenticated user POSTs to `/api-keys/me` with `{"name": "my-key"}`
- **THEN** system generates a unique access key starting with `ak_` and a 32-byte secret key
- **AND** system encrypts the secret key using AES-256-GCM before storing
- **AND** system returns the access key and secret key in the response
- **AND** the secret key SHALL NOT be stored or retrievable after this initial response

#### Scenario: User lists own API keys
- **WHEN** authenticated user GETs `/api-keys/me`
- **THEN** system returns all API keys belonging to the user
- **AND** secret keys SHALL NOT be included in the response

#### Scenario: User enables/disables API key
- **WHEN** authenticated user PUTs `/api-keys/me/{id}/status` with `{"status": "DISABLED"}`
- **THEN** system updates the key status to DISABLED
- **AND** subsequent requests using this key SHALL be rejected with 401

#### Scenario: User deletes API key
- **WHEN** authenticated user DELETEs `/api-keys/me/{id}`
- **THEN** system permanently removes the API key record
- **AND** system removes any cached decrypted secret key from Redis

---

### Requirement: AK/SK creation for system apps
The system SHALL allow administrators to create API keys (AK/SK) for system applications.

#### Scenario: Admin creates system AK/SK
- **WHEN** admin POSTs to `/api-keys/system` with `{"appId": 1, "name": "edge-main-key"}`
- **THEN** system generates a unique access key and secret key
- **AND** system returns the complete key pair in the response
- **AND** the secret key SHALL NOT be stored or retrievable after this initial response

#### Scenario: Admin lists system API keys
- **WHEN** admin GETs `/api-keys/system`
- **THEN** system returns all system-level API keys
- **AND** secret keys SHALL NOT be included in the response

#### Scenario: Admin disables system API key
- **WHEN** admin PUTs `/api-keys/system/{id}/status` with `{"status": "DISABLED"}`
- **THEN** system updates the key status to DISABLED
- **AND** all requests using this key SHALL be rejected

---

### Requirement: AK/SK authentication flow
The system SHALL validate AK/SK credentials for incoming API requests.

#### Scenario: Valid AK/SK authentication
- **WHEN** request contains `X-Access-Key`, `X-Signature`, and `X-Timestamp` headers
- **AND** the access key exists and status is ENABLED
- **AND** the timestamp is within ±5 minutes of server time
- **AND** the signature matches the computed HMAC-SHA256
- **THEN** system SHALL extract the associated user or system app identity
- **AND** system SHALL populate SecurityContext with this identity

#### Scenario: Invalid signature rejection
- **WHEN** request contains valid access key but invalid signature
- **THEN** system SHALL return 401 Unauthorized

#### Scenario: Expired timestamp rejection
- **WHEN** request contains timestamp outside ±5 minutes tolerance
- **THEN** system SHALL return 401 Unauthorized

#### Scenario: Disabled key rejection
- **WHEN** request uses a DISABLED access key
- **THEN** system SHALL return 401 Unauthorized

---

### Requirement: Secret key caching
The system SHALL cache decrypted secret keys in Redis for performance.

#### Scenario: Cache hit on valid key
- **WHEN** AK/SK validation request arrives
- **AND** decrypted SK exists in Redis cache
- **THEN** system SHALL use cached SK for signature verification
- **AND** system SHALL NOT query database or perform decryption

#### Scenario: Cache miss with database fallback
- **WHEN** AK/SK validation request arrives
- **AND** decrypted SK is not in Redis cache
- **THEN** system SHALL query database for encrypted SK
- **AND** system SHALL decrypt SK using AES-256-GCM
- **AND** system SHALL store decrypted SK in Redis with 5-minute TTL
- **AND** system SHALL proceed with signature verification

#### Scenario: Cache invalidation on key disable
- **WHEN** admin disables an API key
- **THEN** system SHALL immediately remove the corresponding cached SK
- **AND** subsequent requests SHALL require re-decryption if re-enabled

---

### Requirement: Simplified signature algorithm
The system SHALL use a simplified signature algorithm for Edge node operations.

#### Scenario: Signature construction for POST
- **WHEN** client constructs signature for `POST /edge/register`
- **THEN** client SHALL build stringToSign as:
  ```
  HTTP_METHOD + "\n" + REQUEST_PATH + "\n" + TIMESTAMP
  ```
- **AND** client SHALL compute `Base64(HMAC-SHA256(stringToSign, SK))`
- **AND** client SHALL set `X-Signature` header to this value

#### Scenario: Signature verification for heartbeat
- **WHEN** Edge node sends heartbeat request
- **THEN** system SHALL verify access key is valid and enabled
- **AND** system SHALL verify timestamp is within tolerance
- **AND** system SHALL compute expected signature and compare

---

### Requirement: Combined authentication with JWT
The system SHALL support both JWT and AK/SK authentication on the same endpoints.

#### Scenario: JWT authentication takes priority
- **WHEN** request contains both `Authorization: Bearer <token>` and `X-Access-Key` headers
- **THEN** system SHALL authenticate using JWT token
- **AND** system SHALL ignore the X-Access-Key header

#### Scenario: AK/SK fallback when no JWT
- **WHEN** request contains only `X-Access-Key` headers (no Bearer token)
- **THEN** system SHALL authenticate using AK/SK credentials

#### Scenario: User context inheritance
- **WHEN** request authenticated via user-level AK/SK
- **THEN** system SHALL inherit the user's role and permissions
- **AND** the authenticated principal SHALL contain the user ID
