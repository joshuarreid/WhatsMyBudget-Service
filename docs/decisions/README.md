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

## Naming Convention

Use sequential numbering and descriptive slugs:

- `ADR-001-some-decision.md`
- `ADR-002-another-decision.md`

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

