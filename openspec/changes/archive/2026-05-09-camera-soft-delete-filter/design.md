## Context

The Camera entity in `aick-mmp-shared` has `is_deleted` and `deleted_at` fields for soft deletion, but lacks a Hibernate-level filter to exclude soft-deleted records from queries. Currently, only some query paths explicitly filter (`getCameras()` via `Specification`, `findAllActive()`, etc.), but most methods (`findAll()`, `findById()`, `findByStatus()`, `countByStatus()`, etc.) return all records including deleted ones.

The `Recording.java` entity already solves this exact problem with `@Where(clause = "is_deleted = false")`, which Hibernate applies to all generated SQL SELECT statements for that entity.

## Goals / Non-Goals

**Goals:**
- Automatically filter out soft-deleted cameras from all Hibernate entity queries
- Keep minimal, focused changes (no refactoring beyond what's needed)
- Follow the existing pattern established by `Recording.java`
- Fix restore/forceDelete/cleanup paths to still access deleted cameras

**Non-Goals:**
- Changing the delete logic from soft-delete to physical delete (that's a separate decision)
- Modifying frontend code
- Adding new API endpoints
- Database schema changes

## Decisions

### 1. Use `@Where` on the entity vs. fixing queries individually

**Decision: Add `@Where(clause = "is_deleted = false")` to Camera entity.**

- **Why**: Mirrors the exact pattern already proven in `Recording.java`. Provides automatic protection for all current and future query paths across all services. Without this, every new query method must remember to explicitly filter — a maintenance trap.
- **Alternatives considered**: Fixing each `findBy*` and service method individually was rejected because it misses queries in other services (AnalyticsService, StreamingService, AlertCheckTask, etc.) and creates no protection for future code.

### 2. Native queries for bypass methods

**Decision: Use `nativeQuery = true` for methods that need to access deleted cameras.**

- **Why**: `@Where` applies to all JPQL/HQL queries but not to native SQL queries. This is the reliable bypass mechanism.
- **Alternatives considered**: Using Hibernate filters (`@FilterDef`) would require explicit filter enablement per session, adding complexity. Removing `@Where` and selectively filtering was rejected per decision #1.

### 3. Remove verification step in `deleteCamera()`

**Decision: Remove the soft-delete verification (lines 191-200) in `CameraServiceImpl.deleteCamera()`.**

- **Why**: With `@Where`, `findById()` will return empty after soft-delete, making the verification always report failure. The code was debug scaffolding and is no longer needed since the `save()` call is immediately below and will throw on failure.

## Risks / Trade-offs

- **[Risk]** **Restore/forceDelete silently fail if called without admin context** → Mitigation: These paths explicitly use native query bypass methods. If a bug is introduced, it will surface as a 404 (ResourceNotFoundException) rather than silent data corruption.
- **[Risk]** **`findAll()` now returns fewer cameras** → Intended. This is the desired behavior. Previously, soft-deleted cameras inflated counts and appeared in listings.
- **[Trade-off]** **`@Where` is deprecated in Hibernate 6 in favor of `@SQLRestriction`** → The behavior is identical and `@Where` continues to work. Both follow the same semantics for entity-level filtering. `@Where` is chosen to stay consistent with `Recording.java`.
- **[Trade-off]** **Native queries bypassing `@Where` are column-name dependent** → Must use database column names (`is_deleted`, `deleted_at`) in native queries instead of Java field names. This is isolated to 2 repository methods, and existing JPQL queries already demonstrate the correct column mapping convention.
