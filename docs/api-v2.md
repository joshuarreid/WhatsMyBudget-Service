# WMB Service – API v2 (JWT Protected)

> **Audience:** React Agent / frontend client
>
> **Base URL (dev):** `http://localhost:8080`
>
> **Auth:** All `/api/v2/**` endpoints require a **Bearer JWT** obtained from `POST /auth/login`.

---

## Authentication

### Generate BCrypt password hash (setup helper)

**POST** `/auth/hash` *(public)*

Request body:
```json
{ "password": "YOUR_PASSWORD" }
```

Response:
```json
{ "bcryptHash": "<bcrypt>" }
```

Use the result to configure the backend:
- Set env var: `WMB_PASSWORD_HASH=<bcryptHash>`

> Recommendation: disable/remove this endpoint after initial setup.

---

### Login (get access token)

**POST** `/auth/login` *(public; rate limited per IP)*

Request body:
```json
{ "password": "YOUR_PASSWORD" }
```

Response body:
```json
{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 86400 }
```

Use on all v2 requests:
- `Authorization: Bearer <accessToken>`

#### Login rate limiting (per IP)

On too many failed attempts:
- HTTP `429 Too Many Requests`
- Header: `Retry-After: <seconds>`

Client IP is extracted using (in order):
1. `X-Forwarded-For` (first IP)
2. `X-Real-IP`
3. socket remote address

Config env vars:
- `WMB_MAX_LOGIN_ATTEMPTS` (default `10`)
- `WMB_LOGIN_LOCK_SECONDS` (default `300`)
- `WMB_ATTEMPT_CACHE_TTL_SECONDS` (default `3600`)
- `WHITELISTED_IPS` (optional, comma-separated; when set, only listed IPs can access the API)

---

## Common Conventions

### Contract and Decision References

- ADR: `docs/decisions/ADR-001-v2-api-contract-parity.md`
- Test coverage guide: `docs/v2-controller-contract-tests.md`

### Headers

- **Request (optional):** `X-Transaction-ID: <string>`
- **Response:** `X-Transaction-ID: <string>`
  - Many endpoints echo caller-provided IDs.
  - Some endpoints may generate a safe ID when the incoming value is missing or invalid.

### Error response shape

Many v2 endpoints return errors as:
```json
{ "status": 500, "code": "SOME_CODE", "message": "...", "transactionId": "..." }
```

> Not all endpoints share the exact same `code` values.

---

## v2 Endpoints

### Budget Transactions (read-only)

Base path: `/api/v2/transactions`

#### List

**GET** `/api/v2/transactions`

Query params (all optional):
- `statementPeriod`
- `startDate` *(ISO date; requires `endDate`)*
- `endDate` *(ISO date; requires `startDate`)*
- `account`
- `category`
- `criticality`
- `criticality_id`
- `paymentMethod`

Notes:
- Provide **either** `statementPeriod` **or** (`startDate` + `endDate`).
- Date ranges are **inclusive**.

Auth:
- `Authorization: Bearer <jwt>`

Response:
- `200 OK` → `BudgetTransactionList`

#### Get by id

**GET** `/api/v2/transactions/{id}`

Auth:
- `Authorization: Bearer <jwt>`

Responses:
- `200 OK` → `BudgetTransaction`
- `404 Not Found`

#### Account view

**GET** `/api/v2/transactions/account`

Query params:
- `account` *(required)*
- `statementPeriod` *(optional)*
- `startDate` *(optional; requires `endDate`)*
- `endDate` *(optional; requires `startDate`)*
- `category` *(optional)*
- `criticality` *(optional)*
- `criticality_id` *(optional)*
- `paymentMethod` *(optional)*

Notes:
- Provide **either** `statementPeriod` **or** (`startDate` + `endDate`).
- Date ranges are **inclusive**.

Response:
- `200 OK` → `AccountBudgetTransactionList`

---

### Projected Transactions (full CRUD)

Base path: `/api/v2/projected-transactions`

#### Create

**POST** `/api/v2/projected-transactions`

Body:
- `ProjectedTransaction` (JSON)
- Supports both `criticality` and `criticality_id`; during transition, responses return both fields.

Responses:
- `201 Created` → `ProjectedTransaction`
- `400 Bad Request`
- `409 Conflict` (duplicate)

#### List

**GET** `/api/v2/projected-transactions`

Query params (all optional):
- `statementPeriod`
- `startDate` *(ISO date; requires `endDate`)*
- `endDate` *(ISO date; requires `startDate`)*
- `account`
- `category`
- `criticality`
- `criticality_id`
- `paymentMethod`

Notes:
- Provide **either** `statementPeriod` **or** (`startDate` + `endDate`).
- Date ranges are **inclusive**.

Response:
- `200 OK` → `ProjectedTransactionList`

#### Get by id

**GET** `/api/v2/projected-transactions/{id}`

Responses:
- `200 OK` → `ProjectedTransaction`
- `404 Not Found`

#### Update

**PUT** `/api/v2/projected-transactions/{id}`

Body:
- `ProjectedTransaction` (JSON)
- Supports both `criticality` and `criticality_id`; during transition, responses return both fields.

Responses:
- `200 OK` → `ProjectedTransaction`
- `400 Bad Request`
- `404 Not Found`
- `409 Conflict` (duplicate)

#### Delete by id

**DELETE** `/api/v2/projected-transactions/{id}`

Responses:
- `204 No Content`
- `404 Not Found`

#### Delete all

**DELETE** `/api/v2/projected-transactions`

Response:
```json
{ "deletedCount": 123 }
```

#### Account view

**GET** `/api/v2/projected-transactions/account`

Query params:
- `account` *(required)*
- `statementPeriod` *(optional)*
- `category` *(optional)*
- `criticality` *(optional)*
- `criticality_id` *(optional)*
- `paymentMethod` *(optional)*

Response:
- `200 OK` → `AccountProjectedTransactionList`

---

### Statement Periods (CRUD)

Base path: `/api/v2/statements`

#### Create

**POST** `/api/v2/statements`

Body:
- `StatementPeriod` (JSON)

Responses:
- `201 Created` → `StatementPeriod`
- `400 Bad Request`
- `409 Conflict` (duplicate)

#### List

**GET** `/api/v2/statements`

Responses:
- `200 OK` → `List<StatementPeriod>`

#### Get by id

**GET** `/api/v2/statements/{id}`

Responses:
- `200 OK` → `StatementPeriod`
- `404 Not Found`

#### Update

**PUT** `/api/v2/statements/{id}`

Body:
- `StatementPeriod` (JSON)

Responses:
- `200 OK` → `StatementPeriod`
- `400 Bad Request`
- `404 Not Found`
- `409 Conflict`

#### Delete by id

**DELETE** `/api/v2/statements/{id}`

Responses:
- `204 No Content`
- `404 Not Found`

#### Delete all

**DELETE** `/api/v2/statements`

Response:
```json
{ "deletedCount": 123 }
```

---

### Budget Limits (account + statement period)

Base path: `/api/v2/budget-limits`

#### Upsert

**PUT** `/api/v2/budget-limits/{account}/{statementPeriod}`

Path params:
- `account` *(required; max 64; letters/numbers/`.`/`_`/`-`)*
- `statementPeriod` *(ignored for writes; kept for backward compatibility)*

Body (`BudgetLimitRequest`):
```json
{
  "essentialLimit": 300.00,
  "nonessentialLimit": 150.00,
  "totalLimit": 500.00
}
```

Notes:
- Any limit field may be omitted or `null`.
- Limits must be `>= 0` and use at most 2 decimal places.
- If `X-Transaction-ID` is missing, blank, `N/A`, or unsafe format, the API generates one.

Responses:
- `200 OK` → `BudgetLimitResponse`
- `400 Bad Request` (`BAD_REQUEST`)
- `500 Internal Server Error` (`UPSERT_ERROR`)

#### Get by account and period

**GET** `/api/v2/budget-limits/{account}/{statementPeriod}`

Responses:
- `200 OK` → `BudgetLimitResponse`
- `400 Bad Request` (`BAD_REQUEST`)
- `404 Not Found` (`NOT_FOUND`)
- `500 Internal Server Error` (`GET_ERROR`)

#### List by period

**GET** `/api/v2/budget-limits`

Query params:
- none required; the UI can call `/api/v2/budget-limits` to fetch all budget rows

Responses:
- `200 OK` → `List<BudgetLimitResponse>`
- `400 Bad Request` (`BAD_REQUEST`)
- `500 Internal Server Error` (`LIST_ERROR`)

`BudgetLimitResponse` shape:
```json
{
  "account": "josh",
  "statementPeriod": "JUNE2026",
  "essentialLimit": 300.00,
  "nonessentialLimit": 150.00,
  "totalLimit": 500.00,
  "createdAt": "2026-08-01T10:15:00",
  "updatedAt": "2026-08-01T11:30:00"
}
```
