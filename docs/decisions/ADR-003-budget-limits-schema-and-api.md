# ADR-003: Budget Limits — Statement-Period-Keyed Schema and v2 API

## Status
Accepted

## Date
2026-08-05

## Context
The app needs to store per-user spending limits (essential, nonessential, total) keyed by
`statement_period` so budget widgets can compare actual spend against limits without duplicating
limit logic in the client or embedding it in transaction queries.

The original "Add budget limits" ticket was split into two sub-tickets:
- **Budget limits database schema** — durable storage layer
- **Budget limits API contract** — v2 REST surface

Key constraints that shaped the design:
- `statement_period` (e.g. `MAY2026`) is already the canonical key across the service; using a
  plain date column would require callers to know the period-to-date mapping.
- Limit fields must be nullable so callers can indicate "no limit set" without setting a value of
  `0`, which would be a valid (zero-dollar) limit.
- The release script must be idempotent and follow the established
  `DELIMITER $$ / DROP PROCEDURE IF EXISTS / CREATE PROCEDURE with IF NOT EXISTS guards / CALL /
  DROP PROCEDURE / DELIMITER ;` pattern used throughout the service.

## Decision

### 1. Database schema (`budget_limits` table)
- Primary key: `id BIGINT AUTO_INCREMENT`
- Composite unique constraint: `(account, statement_period)` — one row per user per period
- Limit columns: `essential_limit`, `nonessential_limit`, `total_limit` — all `DECIMAL(12,2) NULL`
  (null = unconstrained; `0` = explicitly set to zero)
- Audit columns: `created_at`, `updated_at` with `DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE`
- Indexes: `idx_budget_limits_account`, `idx_budget_limits_statement_period`
- DDL delivered via `src/main/resources/sql/release_scripts/budget_limits_release_script.sql`
- `src/main/resources/sql/Schema.sql` updated in parallel to keep the canonical dev/test schema in sync

### 2. v2 API surface (`/api/v2/budget-limits`)
- **PUT** `/api/v2/budget-limits/{account}/{statementPeriod}` — upsert (create or update)
- **GET** `/api/v2/budget-limits/{account}/{statementPeriod}` — fetch by account + period
- **GET** `/api/v2/budget-limits?statementPeriod={statementPeriod}` — list all limits for a period
- `account` path parameter: max 64 chars, alphanumeric + `.`/`_`/`-` only
- `statementPeriod` path/query parameter: `FULL_MONTHYYYY` format enforced at controller
- Limit field validation: `>= 0`, at most 2 decimal places, at most 10 integer digits
- All endpoints echo or generate `X-Transaction-ID` (follows ADR-001 pattern)
- `PUT` uses upsert semantics via `BudgetLimitService.upsert(...)` — idempotent and safe to retry

## Alternatives Considered

### A) Date-based key instead of `statement_period`
- Pros: simpler key type.
- Cons: `statement_period` is already the canonical key across all v2 endpoints; a date key would
  require callers to understand the period boundary logic.
- Rejected: misaligned with the rest of the service contract.

### B) Separate `POST` create and `PATCH` update endpoints
- Pros: standard REST semantics.
- Cons: callers would need to know whether a record exists before choosing the verb; complicates
  client logic for what is a simple "set my limits" operation.
- Rejected: upsert via `PUT` is more ergonomic and idempotent for this use case.

### C) Store limits as zero to represent "no limit"
- Pros: simpler schema (all `NOT NULL`).
- Cons: `0` is a valid and meaningful limit (spending must be zero); callers cannot distinguish
  "no limit" from "zero limit" without a sentinel value or a separate flag column.
- Rejected: nullable columns are the correct semantic for "not set".

## Consequences
- One authoritative row per `(account, statement_period)` — no duplicates possible.
- `PUT` upsert is idempotent; callers can safely retry without creating duplicates.
- Null limit fields mean "unconstrained"; `0` means "zero-dollar limit" — explicit semantic.
- `statement_period` format (`FULL_MONTHYYYY`) enforced at the API layer; downstream code can trust it.
- The release script follows the service's established idempotent migration pattern (safe to
  re-run in dev/CI).

## Related Changes
- `src/main/resources/sql/release_scripts/budget_limits_release_script.sql` *(new)*
- `src/main/resources/sql/Schema.sql` *(updated)*
- `src/main/java/com/example/wmbservice/model/BudgetLimit.java` *(new)*
- `src/main/java/com/example/wmbservice/repository/BudgetLimitRepository.java` *(new)*
- `src/main/java/com/example/wmbservice/service/BudgetLimitService.java` *(new)*
- `src/main/java/com/example/wmbservice/dto/BudgetLimitRequest.java` *(new)*
- `src/main/java/com/example/wmbservice/dto/BudgetLimitResponse.java` *(new)*
- `src/main/java/com/example/wmbservice/controller/BudgetLimitControllerV2.java` *(new)*
- `src/test/java/com/example/wmbservice/service/BudgetLimitServiceTest.java` *(new)*
- `src/test/java/com/example/wmbservice/dto/BudgetLimitRequestValidationTest.java` *(new)*
- `src/test/java/com/example/wmbservice/BudgetLimitControllerV2ContractTest.java` *(new)*
- `docs/api-v2.md` *(updated — budget limits section added)*

## Related Decisions
- `docs/decisions/ADR-001-v2-api-contract-parity.md` — `X-Transaction-ID` propagation pattern applied here
