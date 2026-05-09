## 1. Entity — Camera.java

- [x] 1.1 Add `@Where(clause = "is_deleted = false")` annotation to `Camera.java` entity class

## 2. Repository — CameraRepository.java

- [x] 2.1 Change `findAllDeleted()` JPQL query to `nativeQuery = true` to bypass `@Where`
- [x] 2.2 Add new `@Query(nativeQuery = true, ...)` method `findByIdIncludingDeleted(Long id)` that returns `Optional<Camera>` without the `is_deleted` filter

## 3. Service — CameraServiceImpl.java

- [x] 3.1 In `restoreCamera()`: replace `cameraRepository.findById(id)` with `cameraRepository.findByIdIncludingDeleted(id)`
- [x] 3.2 In `forceDeleteCamera()`: replace `cameraRepository.findById(id)` with `cameraRepository.findByIdIncludingDeleted(id)`
- [x] 3.3 In `deleteCamera()`: remove the soft-delete verification step (lines 191-200) that re-finds the camera after saving

## 4. Verify

- [x] 4.1 Run `mvn compile` to verify the project compiles without errors
- [x] 4.2 Verify existing test compilation errors are pre-existing (unrelated to this change)
