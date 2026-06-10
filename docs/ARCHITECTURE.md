# WMB Service Architecture

## System Overview

`wmbservice` is a Spring Boot API for budgeting data, analytics, projected transactions, statement periods, and payment summaries.

It supports:
- **v1 endpoints** under `/api/**` (legacy/open),
- **v2 endpoints** under `/api/v2/**` (JWT protected),
- auth/token endpoints under `/auth/**`.

## Runtime Topology

- **Application framework:** Spring Boot (`@SpringBootApplication`)
- **Scheduling:** enabled (`@EnableScheduling`) for summary refresh workflows
- **Persistence:** Spring Data JPA repositories (MySQL in runtime; H2 in test profile)
- **Security:** OAuth2 resource server with locally minted JWT tokens

## Layered Design

### 1) Controllers (`controller/`)
Responsibilities:
- HTTP request mapping and parameter parsing
- request validation at boundary level
- response shape and status mapping
- `X-Transaction-ID` propagation

Examples:
- `BudgetTransactionController` and `BudgetTransactionControllerV2`
- `AnalyticsController` and `AnalyticsControllerV2`

### 2) Services (`service/`)
Responsibilities:
- business logic and orchestration
- normalization, deduplication, and domain invariants
- aggregation logic for analytics and summaries

Examples:
- `BudgetTransactionService`, `ProjectedTransactionService`
- `AnalyticsService`, `StatementPeriodSummaryService`
- `PaymentSummaryService`

### 3) Repositories (`repository/`)
Responsibilities:
- persistence operations and query abstraction
- custom query methods for analytics/filtering

Examples:
- `BudgetTransactionRepository`
- `StatementPeriodRepository`
- `ProjectedTransactionRepository`

### 4) Models and DTOs (`model/`, `dto/`)
- `model/` contains entities and response wrappers used across controller/service boundaries
- `dto/` contains analytics-focused payload shapes

## Request Flows

## Flow A: Transaction CRUD
1. Controller accepts request and validates boundary inputs.
2. Service enforces domain rules (e.g., date/period normalization, dedupe checks).
3. Repository persists/queries data.
4. Controller maps results/exceptions to response status/body.

## Flow B: Analytics Query
1. Controller validates period/range inputs.
2. `AnalyticsService` executes aggregate or transformed queries.
3. DTO responses are returned with optional account/joint adjustments.

## Flow C: Statement Period Summaries
1. `StatementPeriodSummaryService` computes or reads summary records.
2. Closed periods are archived/persisted; open periods are computed live.
3. Scheduled refresh updates closed-period summaries.

## Security Architecture

Configured in `config/SecurityConfig.java` and `config/JwtConfig.java`:

- `/auth/login` and `/auth/hash` are public.
- `/api/v2/**` requires JWT authentication.
- `/api/**` is available only when `api.v1.mode` is `active` or `deprecated`; in `disabled` mode the filter returns `410 Gone` centrally.
- Stateless session policy.
- CORS managed via `WebConfig` and security CORS integration.
- JWT signing keys are generated in-memory at startup (tokens invalidate on restart).

## Versioning Strategy

- v1 and v2 controllers coexist.
- v2 parity with v1 behavior is documented in:
  - `docs/decisions/ADR-001-v2-api-contract-parity.md`
  - `docs/v2-controller-contract-tests.md`
- v1 decommissioning is controlled by `api.v1.mode`:
  - `active`: legacy `/api/**` remains fully enabled
  - `deprecated`: legacy endpoints remain enabled and are marked for retirement
  - `disabled`: legacy endpoints are blocked while controller code remains in the codebase (default)
- In `deprecated` mode, v1 responses include deprecation headers:
  - `Deprecation: true`
  - `Sunset: Wed, 31 Dec 2026 23:59:59 GMT`
  - `Link: </api/v2>; rel="successor-version"`
- In `deprecated` mode, v1 request volume is tracked via metric `api.v1.deprecation.hits`.
- In `disabled` mode, legacy `/api/**` requests return `410 Gone` centrally and do not reach controllers.

## Non-Functional Considerations

- **Traceability:** transaction IDs (`X-Transaction-ID`) used across request/response and logs.
- **Test profile isolation:** H2 test config in `src/test/resources/application-test.properties`.
- **Incremental migration:** v2 introduces auth while preserving expected v1 behavior where feasible.

## Key Risks and Follow-Ups

- In-memory JWT keypair is not suitable for production continuity.
- Contract drift risk exists if v1/v2 behavior diverges without parity tests.
- Controller-level error responses are not fully unified across all endpoints.
