---
type: Domain Map
title: AgentCommerce domain module map
description: Domain ownership map for implemented and planned AgentCommerce capabilities.
tags: [domain, modules, backend, ownership]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
  - id: functionality-catalog
    resource: ../../FUNCTIONALITY_CATALOG.md
    title: Functionality catalog
---
# Implemented Foundations

| Domain | Current executable surface |
| --- | --- |
| Identity and access | OIDC/JWT security, customer profile, addresses, consent, devices, emergency contacts |
| Vendor and location | Vendor onboarding, memberships, locations, commerce settings |
| Dasher | Dasher onboarding, verification, zones, availability |
| Catalog and inventory | Catalog item and inventory records, staged CSV validation and commit, customer menu read API |
| Role portals | Customer, vendor, dasher, admin PWAs and REST Lab |

# Planned Domains

| Domain | Planned ownership |
| --- | --- |
| Cart and quote | Vendor-grouped carts, eligibility, immutable quote snapshots, fee calculation |
| Orders | Parent orders, vendor sub-orders, guarded transitions, substitutions, cancellation |
| Payments | Provider abstraction, ledger, refunds, transfers, webhook reconciliation |
| Dispatch and delivery | Offers, assignment, route stops, proof, delivery state |
| Messaging and notification | Conversations, structured proposals, inboxes, push/SMS/email state |
| Announcements and offers | Drafting, approval, eligibility, publishing, redemption |
| Support and feedback | Cases, evidence, refund decisions, escalation |
| Agent governance | Memory, approvals, actions, policies, killswitches |
| Audit and analytics | Append-only evidence and operational projections |

# Ownership Rule

Each domain owns its tables and exposes application interfaces. Cross-domain writes must happen through explicit services or durable events, not direct table writes.
