# Criticality Migration Summary

This file summarizes the database and API changes made to introduce a criticality lookup table and a transitional criticality_id field.

## What changed
- New table: `criticality` (id BIGINT PK, name VARCHAR(32)).
- Seeded rows: 1: Essential, 2: Nonessential, 3: Planned.
- Added `criticality_id` (BIGINT) to `budget_transactions` and `projected_transactions` with FK to `criticality`.
- Backfill/migration script: `src/main/resources/criticality_id_release_script.sql`.
- Release script to create/seed lookup: `src/main/resources/criticality_release_script.sql`.
- `src/main/resources/Schema.sql` updated for fresh DBs (includes `criticality` and seed inserts).

## Java/API changes
- New model: `Criticality` and repository `CriticalityRepository`.
- New service: `CriticalityService` handles normalization and seeding.
- `BudgetTransaction` and `ProjectedTransaction` models now include `criticalityId` mapped to JSON property `criticality_id`.
- Controllers accept query param `criticality_id` (snake_case) and still accept `criticality` (string).
- Services normalize incoming payloads to ensure `criticality` and `criticality_id` match; responses include both fields for transition.
- Repositories updated to allow filtering by `criticality_id`.

## Front-end guidance
- Recommended: send `criticality_id` for new writes (preferred), also include `criticality` string for readability during transition.
- Reads: responses include both `criticality` and `criticality_id`. Use `criticality_id` for exact matching and `criticality` for display.
- Filtering endpoints accept `criticality_id` query param (e.g., `?criticality_id=3`).

## Example payloads
Create transaction (recommended):
```json
{
  "name": "Coffee",
  "amount": 3.50,
  "category": "dining",
  "criticality_id": 3,
  "criticality": "Planned",
  "transactionDate": "2026-06-01",
  "account": "josh",
  "paymentMethod": "visa",
  "statementPeriod": "JUNE2026"
}
```

Create transaction (legacy):
```json
{
  "name": "Rent",
  "amount": 100.00,
  "category": "housing",
  "criticality": "Essential",
  "transactionDate": "2026-06-01",
  "account": "josh",
  "paymentMethod": "visa",
  "statementPeriod": "JUNE2026"
}
```

## Scripts to run (DB admin)
- Run `criticality_release_script.sql` to create and seed lookup table on existing DBs.
- Run `criticality_id_release_script.sql` to add `criticality_id` columns, backfill values from `criticality` strings, add indexes and FKs.
- Fresh DBs: `Schema.sql` now creates the `criticality` table and seeds the three default rows.

## Notes
- Transition is compatibility-first: both fields are supported and returned. Once backfill completes and clients switch to `criticality_id`, consider removing the free-text `criticality` column in a later breaking change.

End of file
