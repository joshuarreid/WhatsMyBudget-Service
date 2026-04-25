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

---

## Common Conventions

### Headers

- **Request (optional):** `X-Transaction-ID: <string>`
- **Response (echoed when provided):** `X-Transaction-ID: <string>`

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
- `account`
- `category`
- `criticality`
- `paymentMethod`

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
- `category` *(optional)*
- `criticality` *(optional)*
- `paymentMethod` *(optional)*

Response:
- `200 OK` → `AccountBudgetTransactionList`

---

### Projected Transactions (full CRUD)

Base path: `/api/v2/projected-transactions`

#### Create

**POST** `/api/v2/projected-transactions`

Body:
- `ProjectedTransaction` (JSON)

Responses:
- `201 Created` → `ProjectedTransaction`
- `400 Bad Request`
- `409 Conflict` (duplicate)

#### List

**GET** `/api/v2/projected-transactions`

Query params (all optional):
- `statementPeriod`
- `account`
- `category`
- `criticality`
- `paymentMethod`

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

