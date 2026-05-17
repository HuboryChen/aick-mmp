## ADDED Requirements

### Requirement: Complete DDL snapshot
The project SHALL maintain a single `schema-full.sql` file containing the complete `CREATE TABLE` statements for all 34 database tables, organized by module.

#### Scenario: Full schema snapshot exists
- **WHEN** a developer opens `schema-full.sql`
- **THEN** it SHALL contain all `CREATE TABLE` statements for every JPA entity in the project
- **THEN** it SHALL group tables by module (shared / central) for readability

#### Scenario: Schema snapshot reflects JPA entities
- **WHEN** any JPA entity is added, modified, or removed
- **THEN** `schema-full.sql` SHALL be updated to reflect the new schema before any incremental migration script is added

### Requirement: Incremental migration scripts
Database changes SHALL be tracked in two forms: the complete snapshot (`schema-full.sql`) and an incremental migration script (`VYYYYMMDD__<description>.sql`).

#### Scenario: Version update workflow
- **WHEN** a developer makes a database schema change
- **THEN** they SHALL first update `schema-full.sql` to reflect the new complete schema
- **THEN** they SHALL create a new incremental migration script in `migration/` with only the changes

#### Scenario: Naming convention
- **WHEN** an incremental migration script is created
- **THEN** its filename SHALL follow the pattern `VYYYYMMDD__<kebab-description>.sql`
- **THEN** the date portion SHALL reflect the date the script was created

### Requirement: Migration script naming cleanup
Existing migration scripts with non-standard naming (`V2__`, `V5__`) SHALL be renamed to the standard `VYYYYMMDD__` format.

#### Scenario: Legacy scripts renamed
- **WHEN** a developer lists files in the `migration/` directory
- **THEN** all 21 scripts SHALL follow the `VYYYYMMDD__` naming convention
- **THEN** the renamed scripts SHALL preserve their original SQL content exactly

### Requirement: Workflow documentation
The `db/` directory SHALL contain a `README.md` describing the DDL management workflow.

#### Scenario: Developer reads workflow docs
- **WHEN** a developer reads `resources/db/README.md`
- **THEN** it SHALL explain: (1) the two-file workflow (full snapshot + incremental), (2) the naming convention, (3) when to update each file
