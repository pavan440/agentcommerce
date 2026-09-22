---
type: Roadmap
title: Implementation gap priority
description: Recommended build order for remaining AgentCommerce vertical slices.
tags: [roadmap, implementation, gaps]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: gap-report
    resource: ../../IMPLEMENTATION_GAP_REPORT.md
    title: Implementation gap report
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
---
# Priority Order

1. Finish catalog and inventory depth: variants, modifiers, allergens, image upload/galleries, item soft delete, timed availability, price history, search, row exclusion, rollback, import history, outbox events.
2. Add cart, quote, reservation, and checkout state machines.
3. Add vendor order handling and substitution coordination.
4. Add payment, ledger, webhook reconciliation, settlement, and refunds.
5. Add dispatch, delivery offers, assignment, route stops, and proof workflows.
6. Add messaging, notifications, support, feedback, and promotions.
7. Add protected agent-tool adapters over completed domain services.
8. Add agent orchestration, evaluations, memory controls, and policy-governed autonomy.
9. Add GCP infrastructure, observability, security hardening, and release automation.

# Build Rule

Complete deterministic domain tools before adding autonomous agent actions over them. The role portals and agents must share the same services so authorization, validation, concurrency, audit, and events cannot diverge.
