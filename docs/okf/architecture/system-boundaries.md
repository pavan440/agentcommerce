---
type: Architecture Boundary
title: AgentCommerce system boundaries
description: Trust, persistence, and runtime boundaries for the AgentCommerce platform.
tags: [architecture, trust-boundary, agents, backend]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
---
# Runtime Shape

| Layer | Responsibility |
| --- | --- |
| Role portals | Customer, vendor, dasher, and admin workflows |
| Domain API | Spring Boot modular monolith and source of deterministic business state |
| PostgreSQL | Transactional records, migrations, and audit-oriented state |
| Future agent orchestration | Role graphs and tool clients that call domain APIs |
| Future providers | Payment, identity, routing, messaging, model inference, and storage adapters |

# Hard Boundaries

- PostgreSQL is the transactional source of truth.
- Domain API services enforce ownership, role, policy, workflow state, validation, concurrency, and audit.
- Agent orchestration has no transactional database credentials.
- Public clients cannot call internal agent-tool routes.
- Provider webhook state is reconciled asynchronously and idempotently.
- Financial records and accepted quote inputs are immutable snapshots.

# Code Generation Implication

Generate code inside the owning module first. Do not create shortcuts in portals, agents, or provider adapters that duplicate business rules outside the domain service.
