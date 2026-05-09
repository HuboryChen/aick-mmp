## 1. Backend Fix: CameraServiceImpl

- [x] 1.1 Update `deleteCamera()` method to set both `isDeleted=true` and `deletedAt`
- [x] 1.2 Update `batchDeleteCameras()` method to set both fields for each camera (uses deleteCamera internally)
- [x] 1.3 Add logging to verify deletion success and persisted state

## 2. Frontend Fix: API Path Correction

- [x] 2.1 Update `batchDeleteCameras` API endpoint from `/cameras/batch-delete` to `/cameras/batch-operation`
- [x] 2.2 Ensure request body format matches backend `BatchOperationDTO` schema

## 3. Verification

- [x] 3.1 Write SQL verification script at `docs/scripts/verify-camera-soft-delete.sql`
- [ ] 3.2 Test single camera deletion end-to-end
- [ ] 3.3 Test batch camera deletion end-to-end
- [ ] 3.4 Verify deleted cameras do not appear in any list query
