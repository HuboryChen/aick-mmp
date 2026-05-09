## Why

Camera deletion (both single and batch) performs a soft delete — it sets `is_deleted=true` and `deleted_at=now()` but never filters them out in queries. Users see "delete successful" messages, yet deleted cameras still appear in listings, can be streamed, trigger alerts, and distort statistics. This is a UX and data integrity bug: deleted cameras should be invisible by default.

## What Changes

- Add `@Where(clause = "is_deleted = false")` to `Camera.java` entity (following the same pattern already used by `Recording.java`)
- Add native query bypass methods in `CameraRepository.java` for operations that need to access deleted cameras (restore, force delete, cleanup task)
- Fix `CameraServiceImpl.java` methods that intentionally work with deleted records (`restoreCamera`, `forceDeleteCamera`, `deleteCamera` verification step)
- No changes to `CameraController.java` endpoints

## Capabilities

### New Capabilities

None — this is an internal data quality fix, not a new feature.

### Modified Capabilities

None — no API contract changes. The external behavior is being corrected to match existing user expectations (deleted = invisible).

## Impact

**Affected files:**
- `backend/aick-mmp-shared/src/main/java/com/aick/mmp/shared/model/Camera.java` — add `@Where` annotation
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/repository/CameraRepository.java` — add native query bypass methods, change `findAllDeleted` to native query
- `backend/aick-mmp-central/src/main/java/com/aick/mmp/central/service/impl/CameraServiceImpl.java` — fix restore/forceDelete to use bypass methods, remove verification step

**Indirectly protected (no changes needed):** All other services that query cameras (StreamingService, AnalyticsService, AnalyticsCollectionTask, AlertCheckTask, EdgeNodeFailoverService, EdgeNodeService, RegionService, RecordingScheduleService) will automatically filter out deleted cameras via the `@Where` annotation.
