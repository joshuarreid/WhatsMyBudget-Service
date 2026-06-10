# ADR-001: Preserve v1 to v2 API Contract Parity for Controllers

## Status
Accepted

## Date
2026-06-10

## Context
The service exposes legacy v1 endpoints under `/api/**` and JWT-protected v2 endpoints under `/api/v2/**`.

Recent controller review found contract drift risk in v2:
- inconsistent propagation of `X-Transaction-ID` in some endpoints,
- weak test coverage for v2-specific validation and error behavior,
- risk of unintentionally changing response semantics while improving v2 internals.

The project requirement is to keep client-visible behavior stable from v1 to v2 unless explicitly documented.

## Decision
Adopt a parity-first policy for v2 controller behavior:

1. Keep v2 status-code and payload semantics aligned with v1 for equivalent endpoints.
2. Preserve existing response body shapes (including places where v1 returns `400` with `null` body) unless an intentional breaking change is approved.
3. Standardize `X-Transaction-ID` handling in v2 controllers where methods execute:
   - echo incoming header when provided,
   - generate and return one when absent or invalid (`null`, blank, `N/A`).
4. Add focused controller contract tests that verify:
   - validation behavior,
   - header propagation,
   - request parsing and filter normalization,
   - representative v1 to v2 parity points.

## Alternatives Considered

### A) Fully redesign v2 error payloads now
- Pros: cleaner and more consistent API shape.
- Cons: introduces avoidable client migration cost and parity break risk.
- Rejected: not aligned with immediate goal of safe parity.

### B) Keep behavior undocumented and rely on code review
- Pros: no documentation overhead.
- Cons: repeated debates and regressions; hidden contract assumptions.
- Rejected: decision context must be explicit for future maintainers.

### C) Duplicate all v1 integration tests for v2 paths
- Pros: strongest parity confidence.
- Cons: high maintenance and slower CI.
- Deferred: use targeted contract tests first, expand where risk is highest.

## Consequences
- Safer incremental improvement path for v2 endpoints.
- Better confidence from explicit contract tests around common regression areas.
- Some inconsistency remains where v1 behavior is intentionally preserved for compatibility.
- Future standardization work should be done behind a separate ADR and versioning plan.
+
+## Related Decisions
+- `docs/decisions/ADR-002-v1-api-decommission-modes.md` — v1 retirement is managed separately through `api.v1.mode`.

## Related Changes
- `src/main/java/com/example/wmbservice/controller/PaymentSummaryControllerV2.java`
- `src/test/java/com/example/wmbservice/PaymentSummaryControllerV2ContractTest.java`
- `src/test/java/com/example/wmbservice/AnalyticsControllerV2ValidationTest.java`
- `src/test/java/com/example/wmbservice/TransactionControllersV2ValidationTest.java`
