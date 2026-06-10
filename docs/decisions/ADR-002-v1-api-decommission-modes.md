# ADR-002: Decommission Legacy v1 APIs with Configurable Modes

## Status
Accepted

## Date
2026-06-10

## Context
The service exposes legacy v1 endpoints under `/api/**` and JWT-protected v2 endpoints under `/api/v2/**`.

The project needs a safe path to retire v1 without deleting controller code immediately. The decommissioning plan must:
- preserve current behavior while the transition is underway,
- allow a visible deprecation phase before cutoff,
- provide a centralized hard stop for legacy requests when v1 is retired,
- keep `/api/v2/**` and `/auth/**` behavior unchanged.

## Decision
Introduce an explicit `api.v1.mode` configuration with three states:

1. `active` — legacy `/api/**` routes continue to function normally.
2. `deprecated` — legacy `/api/**` routes continue to function, but responses include deprecation headers and request volume is tracked with `api.v1.deprecation.hits`.
3. `disabled` — legacy `/api/**` routes are blocked centrally with `410 Gone`, while controller code remains in the repository for rollback and historical reference.

The implementation is centralized in a cross-cutting filter so decommission behavior does not need to be duplicated across controllers.

## Alternatives Considered

### A) Delete v1 controllers immediately
- Pros: smallest runtime surface area.
- Cons: highest migration risk, weak rollback story, removes historical implementation context.
- Rejected: too disruptive for a staged retirement.

### B) Leave v1 open indefinitely
- Pros: no migration work.
- Cons: perpetuates contract drift and duplicated maintenance burden.
- Rejected: contradicts the goal of converging clients on v2.

### C) Add per-controller retirement logic
- Pros: direct and explicit.
- Cons: repetitive, hard to audit, and easy to miss a route.
- Rejected: the behavior should be centralized and consistent across all legacy routes.

## Consequences
- Operators can transition through a visible deprecation phase before a hard cutoff.
- The system retains a rollback path by keeping v1 controller code in place.
- Legacy traffic can be measured before disabling v1 entirely.
- The filter-based approach keeps the decommission policy separate from business logic.

## Related Changes
- `src/main/java/com/example/wmbservice/config/ApiV1Properties.java`
- `src/main/java/com/example/wmbservice/config/ApiV1DeprecationFilter.java`
- `src/main/resources/application.yml`
- `src/test/java/com/example/wmbservice/ApiV1ActiveModeIT.java`
- `src/test/java/com/example/wmbservice/ApiV1DeprecationHeadersIT.java`
- `src/test/java/com/example/wmbservice/ApiV1DisabledModeIT.java`

