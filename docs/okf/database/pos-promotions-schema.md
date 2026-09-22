---
type: Database Schema
title: POS integration, promotions, and community schema
description: POS connections, in-store sales intelligence, promotions, redemptions, announcements, and community zone schema.
tags: [database, pos, promotions, community, sales-intelligence]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: pos-schema
    resource: ../../database/POS_INTEGRATION_AND_SALES_INTELLIGENCE_SCHEMA_DESIGN_V1.md
    title: POS integration and sales intelligence schema design
  - id: promotions-schema
    resource: ../../database/PROMOTIONS_AND_COMMUNITY_SCHEMA_DESIGN_V1.md
    title: Promotions and community schema design
  - id: migration-v5
    resource: ../../../domain-api/src/main/resources/db/migration/V5__create_promotions_and_community_schema.sql
    title: Promotions migration
  - id: migration-v6
    resource: ../../../domain-api/src/main/resources/db/migration/V6__create_pos_integration_schema.sql
    title: POS migration
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| `community_zones` | Hyperlocal geographic marketplace zones |
| `vendor_sales_targets` | Daily and hourly merchant goals and deficit tracking |
| `promotions` | Vendor-authored or agent-drafted offers, funding, schedule, and status |
| `promotion_redemptions` | Customer redemption records for online or in-store claims |
| `promotion_performance` | Attribution and performance metrics |
| `community_announcements` | Vendor announcements and local broadcasts |
| `pos_connections` | Store-to-POS provider mappings, encrypted credential references, sync status |
| `in_store_sales_stream` | Immutable physical register transaction events |
| `inventory_sync_logs` | POS-to-marketplace inventory sync audit trail |
| `hourly_sales_baselines` | Aggregated sales trend baselines for recommendations |

# Generation Rules

- Promotion publishing requires vendor approval and deterministic eligibility checks.
- Offer budgets, stacking, schedule, and audience rules must be enforceable without model judgment.
- POS secrets must be encrypted or provider-tokenized; never expose raw credentials to agents.
- POS ingestion is advisory for sales intelligence until domain services reconcile it into authoritative inventory or promotion workflows.