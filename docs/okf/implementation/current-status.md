---
type: Implementation Status
title: Current implementation status
description: Snapshot of implemented foundations and known incomplete slices in AgentCommerce.
tags: [implementation, audit, status]
status: draft
stale_after: 2026-10-22T00:00:00-07:00
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
  - id: gap-report
    resource: ../../IMPLEMENTATION_GAP_REPORT.md
    title: Implementation gap report
  - id: functionality-catalog
    resource: ../../FUNCTIONALITY_CATALOG.md
    title: Functionality catalog
---
# Implemented Foundations

- OIDC/JWT-secured Spring Boot domain API.
- Customer profile, addresses, consent, devices, and emergency contacts.
- Vendor onboarding, memberships, locations, and commerce settings.
- Dasher onboarding, verification, zones, and availability.
- Catalog and inventory records with vendor-location tenancy.
- Staged UTF-8 CSV inventory validation and explicit commit.
- Customer-facing available-menu API.
- Four responsive installable role PWAs.
- PostgreSQL migrations with PostGIS and pgvector support.
- Unit and PostgreSQL-backed integration testing.

# Under Development

- Rich catalog variants, modifiers, allergens, images, and semantic search.
- Import row exclusion, rollback, object storage, and import history.
- Inventory reservations and outbox events.
- Cart, quote, order, payment, dispatch, messaging, support, and offer services.
- Protected internal agent-tool adapters and the agent orchestration service.
- Production GCP Terraform, Cloud Build, observability, and release automation.

# Freshness Note

The gap report includes older claims that inventory and catalog were not implemented. Current README and source files show an inventory/menu slice now exists. Agents should verify implementation from code and tests before using older audit rows as authoritative.
