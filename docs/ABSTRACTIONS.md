# Core Abstractions

This document maps the main abstractions used in `wmbservice` and how they should be used.

## 1) API Boundary Abstractions

### Controllers
Controllers are boundary adapters between HTTP and domain logic.

Rules:
- Parse and validate request parameters.
- Keep business logic in services.
- Return stable response contracts.
- Propagate `X-Transaction-ID` when possible.

Representative classes:
- `AnalyticsController`, `AnalyticsControllerV2`
- `BudgetTransactionController`, `BudgetTransactionControllerV2`
- `ProjectedTransactionController`, `ProjectedTransactionControllerV2`

## 2) Domain Logic Abstractions

### Services
Services encapsulate business use-cases.

Rules:
- Own domain rules and validation beyond basic request shape.
- Coordinate repository operations and transformations.
- Throw meaningful exceptions consumed by controllers.

Representative classes:
- `BudgetTransactionService`
- `ProjectedTransactionService`
- `AnalyticsService`
- `StatementPeriodSummaryService`
- `PaymentSummaryService`

### Domain Exceptions
Service-local exceptions communicate business failures without leaking persistence details.

Examples:
- `DuplicateTransactionException`
- `ProjectedTransactionNotFoundException`
- `DuplicateStatementPeriodException`

## 3) Persistence Abstractions

### Repositories
Repositories define data access contracts via Spring Data JPA.

Rules:
- Keep query logic in repository methods.
- Avoid embedding SQL-like logic in controllers/services unless unavoidable.
- Return domain entities or query projections suitable for service composition.

Representative classes:
- `BudgetTransactionRepository`
- `ProjectedTransactionRepository`
- `StatementPeriodRepository`

## 4) Data Shape Abstractions

### Entities and Response Models (`model/`)
Used for persistence and many API payloads.

Examples:
- `BudgetTransaction`
- `ProjectedTransaction`
- `StatementPeriod`
- `PaymentSummaryResponse`

### DTOs (`dto/`)
Used for specialized API payloads, primarily analytics.

Examples:
- `AnalyticsPeriodOverviewResponse`
- `AnalyticsCategoryBreakdownResponse`
- `AnalyticsStatementPeriodSummaryResponse`

## 5) Cross-Cutting Abstractions

### Security
Security is abstracted through Spring Security configuration:
- `SecurityConfig` defines route-level auth policy.
- `JwtConfig` defines JWT encoder/decoder and key material.

### CORS
`WebConfig` centralizes CORS behavior across MVC and security filter chains.

### Transaction Traceability
`X-Transaction-ID` acts as request correlation context across logs and responses.

## 6) Versioning Abstractions

API versioning is represented explicitly by parallel controller classes:
- v1: `/api/**`
- v2: `/api/v2/**`

Guideline:
- Preserve behavior parity unless a deliberate breaking change is documented and versioned.
- See `docs/decisions/ADR-001-v2-api-contract-parity.md`.

## 7) Scheduling Abstractions

`StatementPeriodSummaryService` uses scheduled and startup hooks to maintain summary state.

Key distinction:
- **Closed periods:** persisted summary records
- **Open periods:** live computed summary responses

## Practical Guardrails

- Add new domain behavior in services first, then expose via controller.
- Prefer adding tests at controller contract and service logic levels.
- Document abstraction changes with an ADR when they affect public behavior or architecture.

