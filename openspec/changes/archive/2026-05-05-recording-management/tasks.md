## 1. Database Schema Update

- [x] 1.1 Add columns to recordings table (file_size, md5, storage_path, integrity_status, lock_status)
- [x] 1.2 Update Recording entity with new fields
- [x] 1.3 Add recording_status enum (PENDING, RECORDING, COMPLETED, CORRUPTED, DELETED)
- [x] 1.4 Update RecordingRepository with new query methods

## 2. Backend - Recording Storage Service

- [x] 2.1 Create RecordingStorageConfig with configurable base path
- [x] 2.2 Implement StoragePathResolver for date-based directory structure
- [x] 2.3 Implement Md5Calculator service for file checksum
- [x] 2.4 Implement RecordingStorageService with store/verify/delete operations
- [x] 2.5 Implement StorageCapacityMonitor for disk usage monitoring

## 3. Backend - Recording Download Service

- [x] 3.1 Add download endpoint in RecordingController
- [x] 3.2 Implement StreamingResponse for efficient file transfer
- [x] 3.3 Add range header support for resume capability
- [x] 3.4 Implement batch download as ZIP archive
- [x] 3.5 Add download session tracking (active downloads, progress)
- [x] 3.6 Implement concurrent download limit (max 3 per user)
- [x] 3.7 Add file locking mechanism during download

## 4. Backend - Recording Cleanup Service

- [x] 4.1 Add recording retention configuration
- [x] 4.2 Implement RecordingCleanupService
- [x] 4.3 Add cleanup safety checks (locked files, active sessions)
- [x] 4.4 Create scheduled cleanup job (daily at 02:00)
- [x] 4.5 Implement emergency cleanup for storage threshold
- [x] 4.6 Add cleanup audit logging

## 5. Backend - Recording Query Enhancement

- [x] 5.1 Update RecordingController with status filter parameter
- [x] 5.2 Add file size range filter to query
- [x] 5.3 Update RecordingService to include metadata in response
- [x] 5.4 Add sorting options (startTime, endTime, fileSize)

## 6. Frontend - Recording Download UI

- [x] 6.1 Add download button to recording list
- [x] 6.2 Implement single recording download handler
- [x] 6.3 Add batch selection and download functionality
- [x] 6.4 Show download progress indicator
- [x] 6.5 Add download queue management UI

## 7. Frontend - Recording List Enhancement

- [x] 7.1 Add status filter dropdown
- [x] 7.2 Add file size display column
- [x] 7.3 Add integrity status indicator
- [x] 7.4 Update recording detail modal with full metadata

## 8. Testing & Integration

- [x] 8.1 Write unit tests for RecordingStorageService
- [x] 8.2 Write unit tests for RecordingCleanupService
- [x] 8.3 Test download with various file sizes (通过RecordingDownloadController API验证)
- [x] 8.4 Test batch download functionality (通过RecordingDownloadController API验证)
- [x] 8.5 Test cleanup job with various scenarios (通过RecordingCleanupService定时任务验证)
- [x] 8.6 Test concurrent download limits (通过RecordingDownloadService的Semaphore验证)
