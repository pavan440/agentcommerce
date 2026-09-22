---
type: Database Map
title: Database migration map
description: Executable migration order and ownership map for the AgentCommerce PostgreSQL schema.
tags: [database, migrations, postgresql, ownership]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: migrations
    resource: ../../../domain-api/src/main/resources/db/migration
    title: Flyway migrations
  - id: tech-spec
    resource: ../../../TECH_SPEC.md
    title: Technical specification
---
# Migration Order

| Migration | Domain | Primary tables or concern |
| --- | --- | --- |
| `V1__create_identity_schema.sql` | Identity, customer, driver | Users, identities, roles, customer profile, addresses, driver profile, zones, devices, emergency contacts, consents |
| `V2__create_vendor_schema.sql` | Vendor and location | Vendors, memberships, invitations, locations, hours, service areas, commerce settings |
| `V3__create_merchant_schema.sql` | Merchant finance and legal | Merchant legal entity, Stripe Connect account, fee agreements, payouts, bank metadata |
| `V4__create_agent_schema.sql` | Agent governance | Conversations, messages, memories, approvals, policies, actions, killswitches |
| `V5__create_promotions_and_community_schema.sql` | Promotions and community | Community zones, sales targets, promotions, redemptions, performance, announcements |
| `V6__create_pos_integration_schema.sql` | POS integration | POS connections, sales stream, inventory sync logs, hourly sales baselines |
| `V7__create_catalog_inventory_schema.sql` | Catalog and inventory | Catalog items, inventory records, inventory imports, staged import rows |

# Generation Rules

- Add new migrations after the latest version; never rewrite applied migrations for normal feature work.
- Keep ownership aligned with one domain module unless an explicit cross-domain interface exists.
- Use restrictive foreign keys or immutable snapshots where historical commerce records must survive vendor/location closure.
- Use `version` columns for mutable records that participate in optimistic concurrency.
- Use PostGIS types only where spatial queries are required and validate geometry shape at service boundaries.