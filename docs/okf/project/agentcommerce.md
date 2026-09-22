---
type: Project
title: AgentCommerce
description: Agent-native community delivery marketplace with deterministic domain services and role-specific agents.
tags: [agentcommerce, product, marketplace, agents]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
    last_modified: 2026-09-13T00:00:00-07:00
  - id: spec
    resource: ../../../SPEC.md
    title: Product specification
    last_modified: 2026-09-13T00:00:00-07:00
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
    last_modified: 2026-09-13T00:00:00-07:00
---
# Purpose

AgentCommerce is an agent-native local delivery marketplace for customers, vendors, dashers, and platform operators.

The platform combines manual role portals with role-specific AI agents. Agents can recommend and execute approved actions, but they must use the same secured domain APIs as the manual applications.

# Product Rules

- Transactional commerce state is deterministic and auditable.
- Agents do not write directly to databases or bypass authorization, approval, inventory, payment, or workflow rules.
- Customer, vendor, dasher, and operator workflows remain available through manual UI paths.
- Google Cloud Platform is the production target.

# Relationships

- Implementation boundaries are defined in [System boundaries](/architecture/system-boundaries.md).
- Domain ownership is summarized in [Domain map](/domains/domain-map.md).
- Current implementation status is summarized in [Current implementation status](/implementation/current-status.md).
