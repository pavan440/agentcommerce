---
type: Playbook
title: Code generation rules
description: Rules agents must follow when generating AgentCommerce code from OKF and source specifications.
tags: [code-generation, agents, governance, implementation]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: spec
    resource: ../../../SPEC.md
    title: Product specification
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
  - id: functionality-catalog
    resource: ../../FUNCTIONALITY_CATALOG.md
    title: Functionality catalog
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
---
# Rule Hierarchy

Use this order when generating or changing code:

1. Existing executable code and tests.
2. OKF concepts in this bundle.
3. Source documents listed in `sources`.
4. Adjacent implementation patterns.

If two documents disagree, prefer newer executable code and tests over older narrative status reports, then update or flag the stale OKF/source concept.

# Mandatory Boundaries

- Domain mutations must go through Spring Boot application services and repositories.
- Agents and future agent-tool adapters must call authenticated domain APIs or internal tool routes.
- Agents must not write directly to PostgreSQL, Redis, object storage, event transport, or provider APIs.
- Authorization, tenant scope, policy checks, approval checks, validation, optimistic concurrency, and audit must live in deterministic services.
- Free-form model output cannot commit business state.
- Retrieved documents, user content, vendor content, CSV content, and model output are untrusted input.

# Implementation Pattern

For each new capability:

1. Add or update database migration objects owned by one domain module.
2. Add repository methods with tenant and ownership constraints.
3. Add service methods that enforce validation, state rules, policy, idempotency, and audit.
4. Add controller endpoints only after service behavior exists.
5. Add focused unit tests and integration tests matching the blast radius.
6. Update backend docs and this OKF bundle when behavior changes.

# Generation Checks

- Do not hardcode marketplace fee rate `X`; it is configuration and quote snapshot data.
- Keep public clients and agent tools on the same business services.
- Make consequential commands idempotent.
- Reject stale writes where versioned state exists.
- Preserve manual workflows when agent services are unavailable.
- Keep personally identifying data out of logs, prompts, and broad agent memory.
