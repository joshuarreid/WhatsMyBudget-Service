# Analytics API (v1 / legacy)

> **Source of truth:** `AnalyticsController` (`src/main/java/com/example/wmbservice/controller/AnalyticsController.java`)
>
> **Base path:** `/api/analytics`
>
> **Auth:** none (currently unprotected)

This file is written to be machine-ingestible (RAG-friendly): endpoint inventory, parameters, response schemas, and examples.

---

## Conventions

### Content type

- Responses are JSON.

### Tracing header

- Request header (optional): `X-Transaction-ID: <string>`
- Response header: `X-Transaction-ID: <string>` (echoed; server may generate one if absent)

### Date/time formats

- `LocalDate`: ISO-8601 date string, e.g. `"2020-02-15"`
- `LocalDateTime`: ISO-8601 timestamp string, e.g. `"2026-05-24T18:42:55.569027"`

### Error handling

- Many endpoints return `400 Bad Request` for missing/invalid params (blank `period`, invalid date range, unknown statement period, etc.).
- Error response bodies are not standardized for this legacy controller; callers should primarily rely on status code.

### Account filter semantics

- For analytics endpoints that accept an `account` query param, `account=joint` means **joint only**.
- For any non-joint account value (for example `josh`), analytics are calculated as:
  - all transactions whose `account` matches that value
  - plus **50% of all `joint` transactions** in the same filtered dataset
- The 50% split applies to **amounts**.
- Joint transactions included via this rule still contribute to grouped/count metrics as included transactions.
- For account-breakdown endpoints that return multiple account rows, each non-joint account row reflects:
  - that account's direct totals/counts
  - plus the shared 50% joint contribution

---

## Schemas (response shapes)

### `AnalyticsPeriodsResponse`

```json
{
  "periods": ["FEBRUARY2020", "JANUARY2020"],
  "count": 2
}
```

### `AnalyticsPeriodOverviewResponse`

```json
{
  "statementPeriod": "FEBRUARY2020",
  "paymentMethod": null,
  "account": null,
  "totalAmount": 65.00,
  "transactionCount": 2
}
```

Notes:
- For date range overview endpoints, `statementPeriod` is typically `null`.

### `AnalyticsCategoryBreakdownResponse`

```json
{ "category": "groceries", "totalAmount": 40.00, "transactionCount": 1 }
```

### `AnalyticsCriticalityBreakdownResponse`

```json
{ "criticality": "Essential", "totalAmount": 40.00, "transactionCount": 1 }
```

### `AnalyticsAccountBreakdownResponse`

```json
{ "account": "joint", "totalAmount": 40.00, "transactionCount": 1 }
```

### `AnalyticsPaymentMethodBreakdownResponse`

```json
{ "paymentMethod": "visa", "totalAmount": 40.00, "transactionCount": 1 }
```

### `AnalyticsDailyTotalResponse`

```json
{ "date": "2020-02-05", "totalAmount": 40.00, "transactionCount": 1 }
```

### `AnalyticsDuplicateResponse`

```json
{ "rowHash": "abc123", "occurrences": 2, "totalAmount": 120.00 }
```

### `BudgetTransaction` (subset)

The analytics endpoints that return transactions (`/outliers`, `/uncategorized`) return the `BudgetTransaction` entity serialized.
Commonly used fields:

```json
{
  "id": 123,
  "name": "Groceries",
  "amount": 40.00,
  "category": "groceries",
  "criticality": "Essential",
  "transactionDate": "2020-02-05",
  "account": "joint",
  "paymentMethod": "visa",
  "statementPeriod": "FEBRUARY2020",
  "rowHash": "..."
}
```

---

## Endpoints

### 1) Statement periods

#### List available statement periods

`GET /api/analytics/periods`

Response: `AnalyticsPeriodsResponse`

---

### 2) Period analytics (statementPeriod-scoped)

All endpoints below accept:
- Path param: `period` (string; must be non-blank)

#### Overview

`GET /api/analytics/periods/{period}/overview`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `AnalyticsPeriodOverviewResponse`

#### Categories breakdown

`GET /api/analytics/periods/{period}/categories`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsCategoryBreakdownResponse>`

#### Distinct categories (global; no params)

`GET /api/analytics/categories/distinct`

Response: `List<string>` (sorted ascending)

#### Distinct categories

`GET /api/analytics/periods/{period}/categories/distinct`

Response: `List<string>` (sorted ascending)

#### Top categories

`GET /api/analytics/periods/{period}/categories/top`

Query params:
- `limit` (int, default `10`, clamped to `0..100`)
- `paymentMethod` (string, optional)
- `account` (string, optional)

Response: `List<AnalyticsCategoryBreakdownResponse>`

#### Accounts breakdown

`GET /api/analytics/periods/{period}/accounts`

Query params (optional):
- `paymentMethod` (string)

Response: `List<AnalyticsAccountBreakdownResponse>`

#### Payment method breakdown

`GET /api/analytics/periods/{period}/payment-methods`

Query params (optional):
- `account` (string)

Response: `List<AnalyticsPaymentMethodBreakdownResponse>`

#### Criticality breakdown

`GET /api/analytics/periods/{period}/criticality`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsCriticalityBreakdownResponse>`

#### Daily totals

`GET /api/analytics/periods/{period}/daily`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsDailyTotalResponse>`

#### Duplicates (by row hash)

`GET /api/analytics/periods/{period}/duplicates`

Response: `List<AnalyticsDuplicateResponse>`

#### Uncategorized transactions

`GET /api/analytics/periods/{period}/uncategorized`

Response: `List<BudgetTransaction>`

#### Outliers (largest transactions)

`GET /api/analytics/periods/{period}/outliers`

Query params:
- `limit` (int, default `20`, clamped to `0..200`)

Response: `List<BudgetTransaction>`

---

### 3) Range analytics (date-range scoped)

Dates are ISO-8601 (`YYYY-MM-DD`) and ranges are inclusive.

All endpoints below accept query params:
- `startDate` (required)
- `endDate` (required)

#### Overview

`GET /api/analytics/range/overview?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `AnalyticsPeriodOverviewResponse`

#### Categories breakdown

`GET /api/analytics/range/categories?startDate=...&endDate=...`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsCategoryBreakdownResponse>`

#### Top categories

`GET /api/analytics/range/categories/top?startDate=...&endDate=...`

Query params:
- `limit` (int, default `10`, clamped to `0..100`)
- `paymentMethod` (string, optional)
- `account` (string, optional)

Response: `List<AnalyticsCategoryBreakdownResponse>`

#### Accounts breakdown

`GET /api/analytics/range/accounts?startDate=...&endDate=...`

Query params (optional):
- `paymentMethod` (string)

Response: `List<AnalyticsAccountBreakdownResponse>`

#### Payment method breakdown

`GET /api/analytics/range/payment-methods?startDate=...&endDate=...`

Query params (optional):
- `account` (string)

Response: `List<AnalyticsPaymentMethodBreakdownResponse>`

#### Criticality breakdown

`GET /api/analytics/range/criticality?startDate=...&endDate=...`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsCriticalityBreakdownResponse>`

#### Daily totals

`GET /api/analytics/range/daily?startDate=...&endDate=...`

Query params (optional):
- `paymentMethod` (string)
- `account` (string)

Response: `List<AnalyticsDailyTotalResponse>`

#### Duplicates (by row hash)

`GET /api/analytics/range/duplicates?startDate=...&endDate=...`

Response: `List<AnalyticsDuplicateResponse>`

#### Uncategorized transactions

`GET /api/analytics/range/uncategorized?startDate=...&endDate=...`

Response: `List<BudgetTransaction>`

#### Outliers (largest transactions)

`GET /api/analytics/range/outliers?startDate=...&endDate=...`

Query params:
- `limit` (int, default `20`, clamped to `0..200`)

Response: `List<BudgetTransaction>`

---

## Statement-period summaries (persisted/live)

These endpoints return a single JSON payload combining multiple analytics views.

- **Closed periods** are persisted to DB table `statement_period_summaries` and served from the archive.
- **Open periods** are computed live and are not persisted.

### `AnalyticsStatementPeriodSummaryResponse`

Field types:

```text
statementPeriod: string
periodStartDate: string (ISO LocalDate) | null
periodEndDate: string (ISO LocalDate) | null
totalAmount: number
transactionCount: number
essentialAmount: number
essentialCount: number
nonessentialAmount: number
nonessentialCount: number

categoryBreakdown: Map<account, List<{category, totalAmount, transactionCount}>>
criticalityBreakdown: Map<account, List<{criticality, totalAmount, transactionCount}>>
accountBreakdown: Map<account, {account, totalAmount, transactionCount}>
paymentMethodBreakdown: Map<account, List<{paymentMethod, totalAmount, transactionCount}>>
outliers: Map<account, List<BudgetTransaction>>

generatedAt: string (ISO LocalDateTime)
```

Important semantic rules:

- `periodStartDate` / `periodEndDate` are derived from the actual **min/max `transactionDate`** of transactions present in the period.
- Breakdowns are keyed by `BudgetTransaction.account`.
- `outliers[account]` is the top N by `amount` desc (then `transactionDate` desc).

### Get summary by period

`GET /api/analytics/summaries/{period}`

Response: `AnalyticsStatementPeriodSummaryResponse`

Example (shape):

```json
{
  "statementPeriod": "FEBRUARY2020",
  "periodStartDate": "2020-02-05",
  "periodEndDate": "2020-02-15",
  "totalAmount": 65.00,
  "transactionCount": 2,
  "essentialAmount": 40.00,
  "essentialCount": 1,
  "nonessentialAmount": 25.00,
  "nonessentialCount": 1,
  "categoryBreakdown": {
	"joint": [
	  { "category": "groceries", "totalAmount": 40.00, "transactionCount": 1 }
	],
	"josh": [
	  { "category": "clothing", "totalAmount": 25.00, "transactionCount": 1 }
	]
  },
  "criticalityBreakdown": {
	"joint": [
	  { "criticality": "Essential", "totalAmount": 40.00, "transactionCount": 1 }
	],
	"josh": [
	  { "criticality": "Nonessential", "totalAmount": 25.00, "transactionCount": 1 }
	]
  },
  "accountBreakdown": {
	"joint": { "account": "joint", "totalAmount": 40.00, "transactionCount": 1 },
	"josh": { "account": "josh", "totalAmount": 25.00, "transactionCount": 1 }
  },
  "paymentMethodBreakdown": {
	"joint": [
	  { "paymentMethod": "visa", "totalAmount": 40.00, "transactionCount": 1 }
	],
	"josh": [
	  { "paymentMethod": "amex", "totalAmount": 25.00, "transactionCount": 1 }
	]
  },
  "outliers": {
	"joint": [ { "name": "Groceries", "amount": 40.00, "transactionDate": "2020-02-05", "account": "joint" } ],
	"josh": [ { "name": "Shoes", "amount": 25.00, "transactionDate": "2020-02-15", "account": "josh" } ]
  },
  "generatedAt": "2026-05-24T18:42:55.569027"
}
```

### Get summaries by inclusive statement period range

`GET /api/analytics/summaries?startPeriod=...&endPeriod=...`

Response: `List<AnalyticsStatementPeriodSummaryResponse>`

Ordering:
- Inclusive range.
- Sorted by statement period calendar ordering derived from `StatementPeriodSummaryService.resolveBounds(...)`.



