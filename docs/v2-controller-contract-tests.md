# v2 Controller Contract Tests

This document tracks v2 controller contract coverage focused on parity with v1 behavior and request validation safety.

## Purpose

- Prevent regressions in `/api/v2/**` endpoint behavior.
- Keep v2 response contracts stable while implementation evolves.
- Verify traceability header behavior (`X-Transaction-ID`) in controller responses.

## Test Suites

### `PaymentSummaryControllerV2ContractTest`
Path: `src/test/java/com/example/wmbservice/PaymentSummaryControllerV2ContractTest.java`

Covers:
- account list normalization from comma-separated input,
- date parsing validation (`400` on invalid range input),
- blank statement period validation (`400`),
- `X-Transaction-ID` echo behavior,
- generated transaction ID path when no header is provided (indirectly tested in controller behavior).

### `AnalyticsControllerV2ValidationTest`
Path: `src/test/java/com/example/wmbservice/AnalyticsControllerV2ValidationTest.java`

Covers:
- `GET /api/v2/analytics/periods` transaction ID generation and echo,
- `GET /api/v2/analytics/range/overview` invalid date handling (`400`),
- successful date-range parsing and service call semantics.

### `TransactionControllersV2ValidationTest`
Path: `src/test/java/com/example/wmbservice/TransactionControllersV2ValidationTest.java`

Covers:
- `BudgetTransactionControllerV2` invalid date-range request shape (`400`, error contract),
- `ProjectedTransactionControllerV2` invalid date-range request shape (`400`, error contract),
- `X-Transaction-ID` passthrough on error responses.

### `AccountControllerV2ContractTest`
Path: `src/test/java/com/example/wmbservice/AccountControllerV2ContractTest.java`

Covers:
- `GET /api/v2/accounts` returns all accounts with `id` and `accountName` fields,
- empty accounts table returns empty JSON array,
- `X-Transaction-ID` echo behavior.

### `AccountServiceTest`
Path: `src/test/java/com/example/wmbservice/service/AccountServiceTest.java`

Covers:
- `resolveByName()` with known name returns correct `Account`,
- `resolveByName()` is case-insensitive (input is lowercased before lookup),
- `resolveByName()` trims whitespace before lookup,
- `resolveByName()` with unknown name throws `UnknownAccountException`,
- `resolveByName()` with `null` input throws `IllegalArgumentException`,
- `resolveByName()` with blank input throws `IllegalArgumentException`.

### `BudgetTransactionServiceAccountResolutionTest`
Path: `src/test/java/com/example/wmbservice/service/BudgetTransactionServiceAccountResolutionTest.java`

Covers:
- `createTransaction` with known account sets `accountId` and normalizes `account` to lowercase,
- `createTransaction` with unknown account throws `UnknownAccountException` without calling `repository.save()`,
- `updateTransaction` with unknown account throws `UnknownAccountException` without calling `repository.save()`.

### `ProjectedTransactionServiceAccountResolutionTest`
Path: `src/test/java/com/example/wmbservice/service/ProjectedTransactionServiceAccountResolutionTest.java`

Covers:
- `createTransaction` with known account sets `accountId` and normalizes `account` to lowercase,
- `createTransaction` with unknown account throws `UnknownAccountException` without calling `repository.save()`,
- `updateTransaction` with unknown account throws `UnknownAccountException` without calling `repository.save()`.

### `BankStatementServiceAccountResolutionTest`
Path: `src/test/java/com/example/wmbservice/service/BankStatementServiceAccountResolutionTest.java`

Covers:
- `importCreditCardStatement` with unknown account returns `BulkImportResult` with error immediately (zero inserts, no CSV rows processed),
- `importCreditCardStatement` with known account proceeds past account validation into CSV parsing.

## How to Run

```bash
cd "/Users/joshuareid/Documents/Github/wmbservice"

# All contract + account service tests
./mvnw -Dtest="PaymentSummaryControllerV2ContractTest,AnalyticsControllerV2ValidationTest,TransactionControllersV2ValidationTest,AccountControllerV2ContractTest,AccountServiceTest,BudgetTransactionServiceAccountResolutionTest,ProjectedTransactionServiceAccountResolutionTest,BankStatementServiceAccountResolutionTest" test

# Full suite
./mvnw test
```

## Notes

- On this project setup, Mockito inline mock maker can fail under newer JDKs.
- The test resource `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` is configured to `mock-maker-subclass` for compatibility.

## Next Coverage Targets

- Add direct v1-v2 parity tests for summary endpoints (`/analytics/summaries/*`).
- Add account date-range validation tests for `/api/v2/transactions/account` and `/api/v2/projected-transactions/account` for partial-range rejection when behavior is finalized.
- Expand `LocalCacheControllerV2` and `StatementPeriodControllerV2` error contract tests.
- Add integration test for `account_id` dual-write: verify `account_id` is non-null after a successful create via the v2 controller.
- Add integration test for `400 UNKNOWN_ACCOUNT` end-to-end through the full Spring context.

