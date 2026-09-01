# Architecture Decision Records (ADRs)

This folder stores architectural decisions for the `wmbservice` codebase.

## How to Use

- Add one ADR per significant decision.
- Keep old ADRs; do not delete historical records.
- When changing a prior decision, create a new ADR and mark the old one as superseded.

## ADR Index

| ADR | Title | Status | Date | Notes |
|---|---|---|---|---|
| `ADR-001` | v2 API contract parity for controllers | Accepted | 2026-06-10 | Keep v2 behavior aligned with v1, strengthen contract tests |
| `ADR-002` | v1 API decommission modes | Accepted | 2026-06-10 | Stage v1 retirement through active, deprecated, and disabled modes |
| `ADR-003` | budget limits schema and v2 API | Accepted | 2026-08-05 | Add statement-period-keyed persistence and idempotent v2 budget limits endpoints |

## Naming Convention

Use sequential numbering and a descriptive kebab-case slug:

- `ADR-004-short-decision-title.md`
- `ADR-005-another-short-title.md`

## Template

```markdown
# ADR-XXX: Decision title

## Status
Accepted | Proposed | Superseded by ADR-XXX | Deprecated

## Date
YYYY-MM-DD

## Context
What problem are we solving and under what constraints?

## Decision
What we chose.

## Alternatives Considered
Pros/cons and why they were rejected.

## Consequences
Trade-offs, follow-up work, operational impact.
```
