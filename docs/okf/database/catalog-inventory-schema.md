---
type: Database Schema
title: Catalog and inventory schema
description: Current catalog item, single image URL, inventory record, CSV import, and staged-row schema slice.
tags: [database, catalog, inventory, csv-import, images]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: migration-v7
    resource: ../../../domain-api/src/main/resources/db/migration/V7__create_catalog_inventory_schema.sql
    title: Catalog inventory migration
  - id: migration-v8
    resource: ../../../domain-api/src/main/resources/db/migration/V8__add_inventory_image_url.sql
    title: Inventory image URL migration
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
| `catalog_items` | Vendor-location scoped SKU, display name, optional `image_url`, category, price, currency, active status, version |
| `inventory_records` | Tracking mode, quantity on hand, availability, reorder threshold, source, version |
| `inventory_imports` | CSV import lifecycle, summary counts, status, commit metadata |
| `inventory_import_rows` | Staged parsed row data including optional `image_url`, row validation status, committed item reference |

# Image Model

`image_url` is a nullable `VARCHAR(512)` on `catalog_items` and `inventory_import_rows`. It stores externally hosted image metadata only. It does not implement upload, storage, resizing, moderation, galleries, alt text, or ordered media assets.

# Future Normalized Model

Future catalog/inventory work SHOULD split durable item identity from price, availability, and media:

| Concept | Preferred storage direction |
| --- | --- |
| Item identity | `catalog_items` with lifecycle status, soft delete metadata, and audit/version columns |
| Price | `catalog_item_prices` or equivalent effective-dated records; orders snapshot accepted price values |
| Availability | `inventory_records` plus availability state/window fields for out-of-stock and timed pauses |
| Media | `catalog_item_images` backed by object storage; database stores object key, served URL, metadata, status, and sort order; current single-image compatibility may mirror the active URL onto `catalog_items.image_url` |

Do not hard-delete catalog items, price history, inventory history, or media metadata when they may be referenced by orders, carts, imports, audit logs, analytics, or agent actions. Prefer `INACTIVE`, `DELETED`, `OUT_OF_STOCK`, and time-window states.

# Media Storage Lifecycle

Uploaded images should live in object storage. PostgreSQL should store metadata only.

Recommended metadata fields for a future `catalog_item_images` table:

| Field | Purpose |
| --- | --- |
| `id` | Stable image identifier |
| `catalog_item_id` | Owning item |
| `object_key` | Durable GCS/MinIO object reference |
| `image_url` | Served URL or CDN URL derived from object storage |
| `alt_text` | Accessibility/customer context |
| `sort_order` | Gallery order |
| `status` | `ACTIVE`, `PROCESSING`, `REJECTED`, `DELETED` |
| `uploaded_by_user_id` | Vendor/admin actor |
| `created_at`, `updated_at`, `deleted_at`, `version` | Lifecycle and concurrency metadata |

Replace behavior should upload the new object first, then update PostgreSQL so the replacement image is the only `ACTIVE` menu image. The previous metadata row should move to `REPLACED`, `INACTIVE`, or `DELETED`, and the previous object-storage asset should be deleted or scheduled for deletion only after the DB update succeeds.

Delete behavior should remove menu visibility in PostgreSQL and delete or schedule deletion of the object-storage asset. Historical commerce records should keep snapshots or immutable references where required.

# Item, Price, And Availability Lifecycle

Future item status values should include:

- `ACTIVE`: visible and potentially sellable.
- `INACTIVE`: disabled by vendor/admin and hidden from customer ordering.
- `DELETED`: soft-deleted; excluded from normal UI but retained for history.

Future availability values should include:

- `AVAILABLE`: sellable if all other eligibility checks pass.
- `OUT_OF_STOCK`: temporarily not sellable, common for restaurant items.
- `UNAVAILABLE_UNTIL`: unavailable until a specific timestamp.
- `PAUSED_FOR_WINDOW`: unavailable for hours, days, or months.
- `DISCONTINUED`: permanently unavailable for new orders.

Timed availability should support `unavailable_from`, `unavailable_until`, `unavailable_reason`, `auto_restore_at`, and `availability_note`.

Price changes should be versioned or effective-dated. Accepted carts, quotes, and orders must snapshot price inputs so future price updates never mutate historical financial records.

# Price Replace Lifecycle

For the current simple model, changing price updates the active item price and increments item/version metadata. Future normalized pricing should insert a new active/effective price row and close the prior price row with `effective_to` or status metadata. The previous price should remain queryable for audit and historical explanation. Menu reads should return only the currently active/effective price, while accepted quotes/orders keep immutable snapshots.

# Current Limits

- Rich image galleries, uploads/object storage, alt text, variants, modifiers, allergens, semantic search, reservations, item soft delete, timed availability, price history, import rollback, and import history are still under development.
- Customer menu reads expose currently available items, but checkout correctness must still be revalidated by future cart/quote/order services.

# Access Rule

Inventory management is only invokable by active vendor-location managers/owners/admins or users with the platform `OPERATOR` role. Customer menu reads are authenticated read-only and must not expose unavailable items.

# Generation Rules

- Stage CSV rows before mutating live catalog or inventory.
- Use tenant/location ownership checks on every inventory management read and write, except platform `OPERATOR` override workflows.
- Reject stale writes where versions are supplied.
- Treat customer-facing availability as a read projection, not a purchase guarantee; checkout must revalidate item status, availability window, quantity/reservation, and price snapshot inputs.
- Add reservations and outbox events before implementing checkout or autonomous inventory actions at scale.