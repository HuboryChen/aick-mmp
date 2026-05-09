# Implementation Tasks

## 1. Database Schema Changes

- [x] 1.1 Create `system_apps` table migration
- [x] 1.2 Create `api_keys` table migration
- [x] 1.3 Modify `edge_nodes` table - remove `auth_username` and `auth_password` columns
- [x] 1.4 Add `app_id` column to `edge_nodes` table
- [x] 1.5 Add `registered_at` column to `edge_nodes` table
- [x] 1.6 Verify database migrations run successfully (JPA ddl-auto=update)

## 2. Entity Classes

- [x] 2.1 Create `SystemApp` entity in `aick-mmp-shared`
- [x] 2.2 Create `ApiKey` entity in `aick-mmp-shared`
- [x] 2.3 Create `SystemAppPermission` enum with EDGE_REGISTER, EDGE_HEARTBEAT, EDGE_CONFIG_UPDATE
- [x] 2.4 Create `ApiKeyType` enum (USER, SYSTEM)
- [x] 2.5 Create `ApiKeyStatus` enum (ENABLED, DISABLED)
- [x] 2.6 Modify `EdgeNode` entity - remove auth fields, add app_id and registered_at
- [x] 2.7 Create `EdgeNodeRepository` with `findByUuid` method

## 3. Encryption Utilities

- [x] 3.1 Create `AESEncryptionUtil` class with AES-256-GCM support
- [x] 3.2 Add encryption key configuration to `application.yml`
- [x] 3.3 Implement `encrypt(String plaintext)` method
- [x] 3.4 Implement `decrypt(String ciphertext)` method
- [x] 3.5 Write unit tests for encryption utility

## 4. API Key Service

- [x] 4.1 Create `ApiKeyService` interface
- [x] 4.2 Implement `createApiKey(CreateApiKeyRequest)` method
- [x] 4.3 Implement `getDecryptedSecretKey(String accessKey)` with Redis caching
- [x] 4.4 Implement `listApiKeysByUser(Long userId)` method
- [x] 4.5 Implement `listApiKeysByApp(Long appId)` method
- [x] 4.6 Implement `updateApiKeyStatus(Long id, ApiKeyStatus)` method
- [x] 4.7 Implement `deleteApiKey(Long id)` method
- [x] 4.8 Add Redis key invalidation on status change and delete
- [x] 4.9 Write unit tests for ApiKeyService

## 5. System App Service

- [x] 5.1 Create `SystemAppService` interface
- [x] 5.2 Implement `createSystemApp(CreateSystemAppRequest)` method
- [x] 5.3 Implement `listSystemApps(Pageable, SystemAppStatus)` method
- [x] 5.4 Implement `getSystemApp(Long id)` method
- [x] 5.5 Implement `updateSystemApp(Long id, UpdateSystemAppRequest)` method
- [x] 5.6 Implement `deleteSystemApp(Long id)` with validation
- [x] 5.7 Add validation for predefined permissions
- [x] 5.8 Write unit tests for SystemAppService

## 6. Authentication Filter

- [x] 6.1 Create `CombinedAuthFilter` extending `OncePerRequestFilter`
- [x] 6.2 Implement JWT authentication branch
- [x] 6.3 Implement AK/SK authentication branch
- [x] 6.4 Implement signature verification logic
- [x] 6.5 Create `UnifiedPrincipal` class for unified identity
- [x] 6.6 Create `ApiKeyAuthenticationToken` for Spring Security
- [x] 6.7 Integrate filter into `SecurityConfig`
- [x] 6.8 Write unit tests for CombinedAuthFilter

## 7. Signature Verification

- [x] 7.1 Create `SignatureUtil` for signature computation
- [x] 7.2 Implement `verifySignature(String stringToSign, String signature, String secretKey)` method
- [x] 7.3 Implement `buildStringToSign(String method, String path, String timestamp)` method
- [x] 7.4 Add timestamp tolerance check (±5 minutes)
- [x] 7.5 Write unit tests for SignatureUtil

## 8. API Controllers

- [x] 8.1 Create `SystemAppController` with CRUD endpoints
- [x] 8.2 Create `ApiKeyController` for user-level key management
- [x] 8.3 Create `ApiKeyController` for system-level key management
- [x] 8.4 Create `EdgeRegisterController` for self-registration
- [x] 8.5 Add proper authorization (ADMIN role for system operations)
- [x] 8.6 Create DTOs for all request/response objects
- [x] 8.7 Add API documentation with Swagger annotations

## 9. Edge Node Registration Integration

- [x] 9.1 Modify `EdgeNodeController` to use AK/SK authentication
- [x] 9.2 Update heartbeat endpoint to validate Edge via associated SystemApp
- [x] 9.3 Add status management for offline detection
- [x] 9.4 Create scheduled task for heartbeat timeout detection

## 10. Security Configuration Updates

- [x] 10.1 Update `SecurityConfig` to enable method security
- [x] 10.2 Configure `permitAll()` endpoints (login, register)
- [x] 10.3 Configure `authenticated()` endpoints
- [x] 10.4 Add CSRF and CORS configuration

## 11. Edge Node Side Changes

- [x] 11.1 Update Edge node `application.yml` with AK/SK configuration
- [x] 11.2 Create Edge-side signature utility
- [x] 11.3 Modify `EdgeHeartbeatService` to include AK/SK headers
- [x] 11.4 Modify registration flow to use AK/SK
- [x] 11.5 Remove unused `authUsername`/`authPassword` config

## 12. Testing

- [x] 12.1 Write integration tests for AK/SK authentication flow
- [x] 12.2 Write unit tests for AESEncryptionUtil
- [x] 12.3 Write unit tests for SignatureUtil
- [x] 12.4 Write unit tests for ApiKeyService
- [x] 12.5 Write unit tests for SystemAppService
- [x] 12.6 Write unit tests for CombinedAuthFilter

## 13. Documentation Updates

- [x] 13.1 Update ARCHITECTURE.md with new authentication flow
- [x] 13.2 Update AI2AI backend architecture documentation
- [x] 13.3 Update API documentation for new endpoints
- [x] 13.4 Create API key usage guide for users
- [x] 13.5 Create Edge node deployment guide with AK/SK setup
