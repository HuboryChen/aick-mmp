## ADDED Requirements

### Requirement: No hardcoded credentials

The system SHALL NOT contain any hardcoded credentials in source code or committed configuration files. All secrets SHALL be injected via Spring Boot profile-specific configuration files or environment variables.

#### Scenario: Spring Boot profile-based configuration

- **WHEN** a configuration file references a secret value
- **THEN** it SHALL use `${PLACEHOLDER}` syntax in the base `application.yml`
- **AND** the dev profile (`application-dev.yml`, gitignored) SHALL provide local defaults
- **AND** the production profile SHALL resolve values from K8s environment variables
- **AND** a template file (`application-dev.example.yml`, committed) SHALL document required variables

#### Scenario: Global credential rotation

- **WHEN** a global credential needs to be rotated (DB password, Redis password, Kafka SASL credential, JWT secret, camera encryption key)
- **THEN** the operator SHALL update the K8s Deployment environment variable
- **AND** perform a rolling restart of the pods
- **AND** zero downtime SHALL be maintained during rotation (rolling update)

### Requirement: Camera credential encryption

Camera access credentials (passwords for RTSP/ONVIF connection) SHALL be encrypted at rest in the database using AES-256-GCM, with a dedicated encryption key independent from other system secrets.

#### Scenario: Storage and encryption

- **WHEN** a camera password is stored in the `cameras.password` column
- **THEN** it SHALL be encrypted using AES-256-GCM (AES/GCM/NoPadding)
- **AND** the ciphertext SHALL be stored as Base64 (`[12-byte IV][ciphertext]`)
- **AND** the `cameras.username` column MAY remain in plaintext (non-sensitive)
- **AND** the encryption key SHALL be configured via `security.encryption.camera-credential-key` (separate from SystemApp encryption key)

#### Scenario: Transparent encryption via JPA AttributeConverter

- **WHEN** the application writes a camera entity via JPA
- **THEN** `CameraPasswordEncryptor` (JPA `AttributeConverter`) SHALL automatically encrypt the password before write
- **AND** `camera.getPassword()` SHALL always return the decrypted plaintext to business code
- **AND** no service-layer code SHALL need to call encryption/decryption methods directly

#### Scenario: API response masking

- **WHEN** the management UI queries camera details (`GET /api/cameras` or `GET /api/cameras/{id}`)
- **THEN** the `password` field in the response SHALL be masked as `******`
- **WHEN** the edge node queries its assigned cameras (`GET /api/cameras/edge-node/{nodeId}`)
- **THEN** the `password` field SHALL return the decrypted plaintext (required for RTSP/ONVIF connection)

#### Scenario: Cache with invalidation

- **WHEN** a camera password is decrypted for the edge node API
- **THEN** the plaintext result SHALL be cached in Redis with key `camera:pwd:decrypted:{cameraId}`
- **AND** the cache TTL SHALL be 1 hour
- **WHEN** a camera password is updated via `PUT /api/cameras/{id}`
- **THEN** the corresponding Redis cache entry SHALL be immediately invalidated

#### Scenario: Password rotation per camera

- **WHEN** an operator updates a camera's password in the management UI
- **THEN** the `PUT /api/cameras/{id}` endpoint SHALL accept the new plaintext password
- **AND** the JPA Converter SHALL encrypt the new value before persistence
- **AND** the Redis cache SHALL be invalidated immediately
- **AND** the edge node SHALL receive the new password on the next camera list pull
- **AND** the entire rotation SHALL complete without service restart

#### Scenario: Migration of existing plaintext passwords

- **WHEN** the application starts after the encryption change is deployed
- **THEN** a Flyway Java-based migration SHALL:
  - Read all `cameras.password` values
  - Detect encryption status (already encrypted ciphertext starts with a 16-character Base64 IV = 12 bytes)
  - Encrypt any remaining plaintext passwords using AES-256-GCM
  - Update the `password` column with the encrypted value
- **AND** the migration SHALL be idempotent (re-running does not double-encrypt)
