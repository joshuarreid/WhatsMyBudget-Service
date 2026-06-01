<overview>
User requested a schema and migration to add a Criticality lookup table and a criticality_id column for budget_transactions and projected_transactions. The approach was compatibility-first: introduce a criticality table (seeded with Essential, Nonessential, Planned), add criticality_id columns, backfill IDs from existing criticality text, and update the Java models, repositories, services, and controllers so APIs accept and return both criticality and criticality_id.
</overview>

<history>
1. User requested a plan to add a new criticality table and migration scripts, and asked whether endpoints/DTOs should return criticality_id and remove criticality.
   - Inspected repository, resources/Schema.sql and Java models/services to determine where criticality is used.
   - Recommended a compatibility-first approach: keep existing criticality string while introducing criticality_id and synchronizing both fields.

2. User asked to execute the plan.
   - Implemented Java model, repository, and service changes to support criticality_id while preserving criticality.
   - Added Criticality JPA entity, CriticalityRepository, and CriticalityService to seed defaults and normalize transaction objects.
   - Updated BudgetTransaction and ProjectedTransaction models to include criticality_id with JSON aliases for both snake_case and camelCase.
   - Updated repositories' query methods to allow filtering by criticality_id and to include the new column in repository queries.
   - Updated controllers to accept an optional criticality_id query parameter and pass it through to services.
   - Adjusted services to normalize/seed criticality and maintain both fields on create/update and when splitting joint transactions.

3. Database artifacts and scripts:
   - Updated Schema.sql to add a criticality table, seed it with three rows (Essential=1, Nonessential=2, Planned=3), and add criticality_id columns and FK/indexes to both transaction tables.
   - Added two release scripts: criticality_release_script.sql (creates/seeds criticality) and criticality_id_release_script.sql (adds criticality_id columns, backfills values from existing criticality text, adds indexes and FKs).
   - Updated sample-data.sql to include criticality_id values for inserted rows.

4. Tests and docs:
   - Updated API docs (docs/api-v2.md) to document criticality_id as additional filter/field.
   - Updated a couple integration tests to set criticality_id and to assert presence/behavior.

5. Ran the test suite multiple times; fixed wiring issues and ensured tests pass locally (H2 in-memory test profile).
</history>

<work_done>
Files created:
- src/main/java/com/example/wmbservice/model/Criticality.java: JPA entity for lookup table.
- src/main/java/com/example/wmbservice/repository/CriticalityRepository.java: Spring Data repository.
- src/main/java/com/example/wmbservice/service/CriticalityService.java: seeds defaults and normalizes transaction objects.
- src/main/resources/criticality_release_script.sql: idempotent script to create and seed criticality.
- src/main/resources/criticality_id_release_script.sql: migration procedure to add criticality_id, backfill existing rows, add indexes and FKs.
- src/main/resources/criticality_migration_summary.md (this file): human-readable summary of changes for frontend context.

Files modified:
- src/main/resources/Schema.sql: added criticality table and seed rows; added criticality_id columns, indexes, and FK constraints for budget_transactions and projected_transactions.
- src/main/resources/sample-data.sql: added criticality_id values for sample rows.
- src/main/java/com/example/wmbservice/model/BudgetTransaction.java: added criticalityId field with JSON alias and toString change.
- src/main/java/com/example/wmbservice/model/ProjectedTransaction.java: added criticalityId field and JSON aliases.
- src/main/java/com/example/wmbservice/repository/BudgetTransactionRepository.java: extended queries to accept criticalityId parameter and filter by it.
- src/main/java/com/example/wmbservice/repository/ProjectedTransactionRepository.java: same changes for projected repository.
- src/main/java/com/example/wmbservice/service/BudgetTransactionService.java: integrated CriticalityService, normalized inputs, updated find/query call sites to include criticalityId.
- src/main/java/com/example/wmbservice/service/ProjectedTransactionService.java: same as above for projected transactions.
- src/main/java/com/example/wmbservice/controller/*Controller*.java: controllers updated to accept optional criticality_id query param (snake_case) and forward it to services.
- docs/api-v2.md and docs/AnalyticsAPI.md: documented criticality_id and that APIs return both fields during transition.
- src/test/java/...: adjusted tests to include criticality_id and asserted expected behavior.

Tasks completed:
- [x] Add criticality table and seed rows (Essential/Nonessential/Planned).
- [x] Add criticality_id columns to budget_transactions and projected_transactions (schema and release script)
- [x] Backfill criticality_id from existing criticality text via migration script.
- [x] Update Java models, repositories, services, and controllers to accept/return/operate with criticality_id.
- [x] Update docs and sample data.
- [x] Run tests and ensure application boots in test profile.

Current state:
- Application compiles and tests run locally under H2 (test profile). Controllers and services accept both criticality and criticality_id; outputs include both fields.
- Migration scripts provided for DB administrators to run in production.
- Frontends can transition by sending criticality_id (preferred) or criticality (legacy) — the service validates and synchronizes both.

Known issues / untested areas:
- End-to-end migration on a production-sized MySQL instance was not executed here; the release scripts are idempotent but should be tested on a staging DB.
- Some controllers and secondary services were updated; manual QA of all API endpoints consuming criticality may be necessary.
</work_done>

<technical_details>
- Compatibility-first decision: keep the text field `criticality` and add `criticality_id` to avoid breaking API consumers. Services normalize and ensure both fields are consistent.
- Criticality table seeded ids: 1=Essential, 2=Nonessential, 3=Planned. CriticalityService seeds these defaults at application startup using @PostConstruct.
- Migration strategy: provide two SQL artifacts:
  1) criticality_release_script.sql — ensures criticality lookup exists and seeded.
  2) criticality_id_release_script.sql — adds criticality_id columns to transaction tables, backfills IDs by joining on LOWER(TRIM(text)), adds indexes and foreign keys.
- API contract: endpoints now accept `criticality` (string) and `criticality_id` (snake_case query param). JSON responses include both `criticality` and `criticality_id`. The JSON mapping includes aliases (camelCase and snake_case) to ease frontend transition.
- Repository queries: updated custom JPQL/native queries to include optional `criticalityId` parameter alongside the text filter.
- Tests: project uses H2 in MySQL compatibility mode in tests; migrations and schema changes were exercised in this context — watch for MySQL-specific behaviors in production (e.g., index/constraint name handling).
- Quirk: some parts of the code compute dedupe row hashes including `criticality` as a string; the code continues to include string criticality in hash logic. If the team prefers dedupe by id, row-hash logic would need to be updated consistently.
- Assumptions:
  - Existing `criticality` values in DB map to seeded names exactly (ignoring case/whitespace). The backfill uses case-insensitive trimmed matching; mismatches will leave criticality_id NULL and should be handled manually.
  - Application startup seeding is safe for environments where the service can write to criticality table; in read-only or locked DBs use release scripts instead.

Questions / open decisions:
- Whether to fully deprecate the textual `criticality` column and remove it in a future breaking release (recommended once all frontends use criticality_id).
- Whether row-hash deduplication should be changed to use criticality_id instead of text — this affects dedup behavior and should be coordinated with data migration.
</technical_details>

<important_files>
- src/main/resources/Schema.sql
  - Why: canonical DDL used by maintainers; updated to include criticality table and criticality_id in both transaction tables.
  - Changes: added criticality table, insert seed rows, added criticality_id column definitions, indexes, and FK constraints.
  - Key lines: criticality table creation and INSERT seed block near top; budget_transactions and projected_transactions column lists.

- src/main/resources/criticality_id_release_script.sql
  - Why: idempotent migration procedure to add columns and backfill values on existing production DB.
  - Changes: new file; contains statements to add columns, backfill by matching text, add indexes and FKs.
  - Key parts: UPDATE ... JOIN criticality ON LOWER(TRIM(text)) = LOWER(TRIM(name))

- src/main/java/com/example/wmbservice/model/BudgetTransaction.java and ProjectedTransaction.java
  - Why: primary domain models; now include criticalityId and JSON aliases so frontends can send/receive either field.
  - Changes: added Long criticalityId with @JsonProperty("criticality_id") and @JsonAlias("criticalityId").

- src/main/java/com/example/wmbservice/service/CriticalityService.java
  - Why: seeds defaults and centralizes normalization/resolution logic for criticality name ↔ id mapping.
  - Changes: new service with @PostConstruct seeding and resolve/normalize helpers.

- src/main/java/com/example/wmbservice/repository/*TransactionRepository.java
  - Why: queries updated to accept optional criticalityId filter to support numeric filtering.

- docs/api-v2.md
  - Why: API consumers must know the new optional query param and response fields.
  - Changes: documented `criticality_id` and that responses include both fields during transition.
</important_files>

<next_steps>
Remaining tasks / recommendations:
- Run the provided release scripts (criticality_release_script.sql then criticality_id_release_script.sql) on a staging DB and validate backfill correctness.
- Decide a migration window and deprecation plan for the textual `criticality` column (e.g., mark deprecated, then drop after all clients migrate).
- Consider updating deduplication/row-hash logic to use criticality_id if that better represents canonical identity.
- Update frontend clients to prefer `criticality_id` (send as `criticality_id` in query params and payloads), and continue to accept `criticality` until full migration.
- Additional QA: exercise all analytics endpoints, filters, and reports relying on criticality breakdowns to ensure behavior unchanged.

Immediate next action to continue work:
- Provide sample frontend payload examples showing both forms (string-only, id-only, both) and indicate preferred pattern (send id; include name optional).

</next_steps>

<checkpoint_title>Criticality lookup migration</checkpoint_title>
