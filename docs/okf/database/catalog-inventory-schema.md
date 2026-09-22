---
type: Database Schema
title: Catalog and inventory schema
description: Current catalog item, inventory record, CSV import, and staged-row schema slice.
tags: [database, catalog, inventory, csv-import]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: migration-v7
    resource: ../../../domain-api/src/main/resources/db/migration/V7__create_catalog_inventory_schema.sql
    title: Catalog inventory migration
  - id: inventory-api-okf
    resource: ../apis/inventory-api.md
    title: Inventory API OKF concept
  - id: gap-report
    resource: ../../IMPLEMENTATION_GAP_REPORT.md
    title: Implementation gap report
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| `catalog_items` | Vendor-location scoped SKU, display name, category, price, currency, active status, version |
| `inventory_records` | Tracking mode, quantity on hand, availability, reorder threshold, source, version |
| `inventory_imports` | CSV import lifecycle, summary counts, status, commit metadata |
| `inventory_import_rows` | Staged parsed row data, row validation status, committed item reference |

# Current Limits

- Rich variants, modifiers, allergens, images, semantic search, reservations, import rollback, object storage, and import history are still under development.
- Customer menu reads expose currently available items, but checkout correctness must still be revalidated by future cart/quote/order services.

# Generation Rules

- Stage CSV rows before mutating live catalog or inventory.
- Use tenant/location ownership checks on every read and write.
- Reject stale writes where versions are supplied.
- Treat customer-facing availability as a read projection, not a purchase guarantee.
- Add reservations and outbox events before implementing checkout or autonomous inventory actions at scale.