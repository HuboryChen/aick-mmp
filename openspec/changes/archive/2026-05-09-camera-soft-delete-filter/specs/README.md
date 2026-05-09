# Camera Soft Delete Filter — Specs

This change has no new or modified capabilities. It is an internal data quality fix
that adds `@Where(clause = "is_deleted = false")` to the Camera entity. The external
API behavior is unchanged — the fix only ensures that soft-deleted cameras are
actually filtered out from all queries as users already expected.
