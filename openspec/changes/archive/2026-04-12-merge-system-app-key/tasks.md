## 1. Database Migration

- [x] 1.1 Add migration script for system_apps table (app_key, encrypted_secret, last_used_at columns)
- [x] 1.2 Create data migration script to copy system API keys to SystemApp
- [x] 1.3 Create cleanup script to remove SYSTEM type records from api_keys table
- [x] 1.4 Test migration scripts locally

## 2. Backend - Model Layer

- [x] 2.1 Modify SystemApp entity: add app_key, encryptedSecret, lastUsedAt fields
- [x] 2.2 Remove systemApp relationship from ApiKey entity (keep only userId)
- [x] 2.3 Update ApiKeyType enum if needed (remove SYSTEM type)

## 3. Backend - Repository Layer

- [x] 3.1 Update SystemAppRepository to add appKey lookup method
- [x] 3.2 Update ApiKeyRepository to remove system app relationship queries

## 4. Backend - Service Layer

- [x] 4.1 Refactor SystemAppServiceImpl:
  - [x] 4.1.1 Add key generation logic (generateAppKey, generateAppSecret)
  - [x] 4.1.2 Add credential encryption/decryption methods
  - [x] 4.1.3 Add credential retrieval logic with one-time display
  - [x] 4.1.4 Add credential regeneration logic
  - [x] 4.1.5 Add authentication validation method
- [x] 4.2 Refactor ApiKeyServiceImpl:
  - [x] 4.2.1 Remove SYSTEM type key creation
  - [x] 4.2.2 Remove system API key listing
  - [x] 4.2.3 Keep only USER type key operations
- [x] 4.3 Update SignatureUtil for app authentication

## 5. Backend - Controller Layer

- [x] 5.1 Update SystemAppController:
  - [x] 5.1.1 Modify create endpoint to return credentials on creation
  - [x] 5.1.2 Add credential retrieval endpoint
  - [x] 5.1.3 Add credential regeneration endpoint
- [x] 5.2 Update ApiKeyController:
  - [x] 5.2.1 Remove system API key endpoints (/api-keys/system/*)
  - [x] 5.2.2 Keep user API key endpoints (/api-keys/me/*)
- [x] 5.3 Update security configuration for new endpoints

## 6. Backend - DTO Layer

- [x] 6.1 Update SystemAppDTO to include credentials info
- [x] 6.2 Create SystemAppCredentialsResponseDTO
- [x] 6.3 Update CreateSystemAppRequestDTO if needed

## 7. Frontend

- [x] 7.1 Update SystemApp management page to show credentials on creation
- [x] 7.2 Add credential retrieval button for existing apps
- [x] 7.3 Add credential regeneration confirmation dialog
- [x] 7.4 Remove standalone system API key management page
- [x] 7.5 Update API service calls

## 8. Testing

- [ ] 8.1 Write unit tests for SystemAppService key operations
- [ ] 8.2 Write unit tests for ApiKeyService user operations
- [ ] 8.3 Test migration scripts with sample data
- [ ] 8.4 Update existing integration tests

## 9. Documentation

- [x] 9.1 Update API documentation for changed endpoints
- [x] 9.2 Update OpenAPI specification
- [x] 9.3 Update AI2AI/协议和数据.md with new database schema
