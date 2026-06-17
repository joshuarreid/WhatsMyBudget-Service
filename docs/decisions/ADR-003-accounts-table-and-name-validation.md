# ADR-003: Introduce `accounts` Table as Canonical Account Registry with Name Validation on Writes

## Status
Accepted

## Date
2026-06-17

## Context

`BudgetTransaction` and `ProjectedTransaction` stored account identity as a plain `VARCHAR(32)` column (`account`). This had several consequences:

- Account names were free-form strings with no enforcement: `"Josh"`, `"josh"`, `"JOSH"` could coexist as separate values.
- No canonical list of valid accounts existed; the frontend had to hardcode dropdown values.
- Joint-account split logic in `BudgetTransactionService`, `ProjectedTransactionService`, `AnalyticsService`, and `PaymentSummaryService` all relied on the literal string `"joint"` — a silent, unchecked convention.
- There was no FK relationship allowing future per-account features (income, goals, habits) to link cleanly to a stable identity.
- The database already had `account_name` strings that could drift from each other across tables.

The immediate requirement was to add income tracking and goals, which need a stable account FK as an owner. A separate full user-model migration was deferred.

## Decision

1. **Add an `accounts` table** with `id BIGINT` PK and `account_name VARCHAR(32) UNIQUE`.
2. **Dual-write `account_id`** on every transaction write (create/update/bulk-import) while keeping the legacy `account` VARCHAR column intact for all read, filter, and analytics paths.
3. **Fail-fast 400 on unknown account names** at the service layer (`AccountService.resolveByName()`), before any DB write occurs. This prevents silent FK-null rows.
4. **Case-insensitive, trim-normalized matching**: input is always lowercased + trimmed before lookup; the normalized name is written back to the `account` column.
5. **Expose `GET /api/v2/accounts`** so frontends can populate account dropdowns dynamically instead of hardcoding values.
6. New accounts cannot be created via the API — they are managed directly in the `accounts` table (admin operation).

## Alternatives Considered

### A) Migrate `account` to an FK immediately (drop string column)
- Pros: clean single-column ownership.
- Cons: requires updating all JPQL queries, analytics aggregation, CSV import, test fixtures, and frontend contracts in one change.
- Rejected: too high-risk to ship atomically; the dual-write approach allows migration in stages.

### B) Case-insensitive matching without fail-fast (auto-create unknown accounts)
- Pros: zero friction for callers; any name is accepted.
- Cons: typos silently create orphan account rows; the allowlist becomes meaningless.
- Rejected: fails the goal of a canonical registry; `joint` split logic would silently miss misspelled names.

### C) Validate account in controllers only (not service layer)
- Pros: simpler service interface.
- Cons: `BankStatementService` and `BudgetTransactionCsvImporter` bypass controllers; validation would have to be duplicated or silently skipped.
- Rejected: validation must live in the service layer so all write paths are covered regardless of entry point.

### D) Validate account only in v2 controllers, not v1
- Pros: targeted change.
- Cons: v1 bulk-import paths (`/upload`, `/upload-statement`) would still write null `account_id` rows.
- Rejected: the dual-write invariant must hold for all inserts or the migration is unsafe.

## Consequences

**Immediate:**
- All transaction writes (single create/update, CSV bulk import, bank statement upload) now validate account names against `accounts` and write `account_id`.
- Unknown account names return `400 UNKNOWN_ACCOUNT` from v2 controllers.
- `account` string column is normalized to lowercase-trim on every write; existing stored values are unchanged.
- `account_id` is `@JsonIgnore` — invisible to all existing API responses; no frontend contract change.
- Analytics, filtering, and joint-split logic continue to operate on the `account` VARCHAR — no behavior change there.

**Migration path (next steps):**
- Once all rows have non-null `account_id` (verify via: `SELECT COUNT(*) FROM budget_transactions WHERE account_id IS NULL`), add `NOT NULL` constraint and FK.
- Later: flip read paths to join through `accounts` via `account_id` instead of filtering by `account` string.
- Later: add income, goals, habits tables with `account_id FK → accounts(id)`.

**Risks:**
- Existing rows written before this change have `account_id = NULL`. These rows remain readable but are not yet validated. Track via the query above.
- Test fixtures that save transactions directly via the repository bypass `AccountService` and will have `account_id = NULL`. This is acceptable during the transition period.

## Related Decisions
- `docs/decisions/ADR-001-v2-api-contract-parity.md` — error shape contract for `400` responses is preserved.
- Future ADR needed when read paths are flipped to use `account_id`.
- Future ADR needed when per-user features (income, goals) are added.

## Related Changes
- `src/main/java/com/example/wmbservice/model/Account.java` *(new)*
- `src/main/java/com/example/wmbservice/repository/AccountRepository.java` *(new)*
- `src/main/java/com/example/wmbservice/service/AccountService.java` *(new)*
- `src/main/java/com/example/wmbservice/controller/AccountControllerV2.java` *(new)*
- `src/main/java/com/example/wmbservice/model/BudgetTransaction.java` — added `accountId` field
- `src/main/java/com/example/wmbservice/model/ProjectedTransaction.java` — added `accountId` field
- `src/main/java/com/example/wmbservice/service/BudgetTransactionService.java` — account resolution in create/update/bulk-import
- `src/main/java/com/example/wmbservice/service/ProjectedTransactionService.java` — account resolution in create/update
- `src/main/java/com/example/wmbservice/service/BankStatementService.java` — fail-fast account validation before CSV parse loop
- `src/main/java/com/example/wmbservice/controller/BudgetTransactionControllerV2.java` — catch `UnknownAccountException` → `400`
- `src/main/java/com/example/wmbservice/controller/ProjectedTransactionControllerV2.java` — catch `UnknownAccountException` → `400`
- `src/test/java/com/example/wmbservice/service/AccountServiceTest.java` *(new)*
- `src/test/java/com/example/wmbservice/service/BudgetTransactionServiceAccountResolutionTest.java` *(new)*
- `src/test/java/com/example/wmbservice/service/ProjectedTransactionServiceAccountResolutionTest.java` *(new)*
- `src/test/java/com/example/wmbservice/service/BankStatementServiceAccountResolutionTest.java` *(new)*
- `src/test/java/com/example/wmbservice/AccountControllerV2ContractTest.java` *(new)*
- `docs/api-v2.md` — added `GET /api/v2/accounts` endpoint documentation

