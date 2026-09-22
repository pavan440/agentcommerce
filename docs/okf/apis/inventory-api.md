---
type: API Surface
title: Inventory and customer menu API
description: Current API slice for vendor/admin inventory import, inventory management, image URL metadata, and customer menu display.
tags: [api, inventory, catalog, customer-menu, vendor, operator, images]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: readme
    resource: ../../../README.md
    title: AgentCommerce README
  - id: inventory-doc
    resource: ../../backend/INVENTORY_API.md
    title: Inventory API documentation
  - id: inventory-controller
    resource: ../../../domain-api/src/main/java/com/agentcommerce/domain/inventory/InventoryController.java
    title: InventoryController
  - id: inventory-service
    resource: ../../../domain-api/src/main/java/com/agentcommerce/domain/inventory/InventoryService.java
    title: InventoryService
  - id: upsert-request
    resource: ../../../domain-api/src/main/java/com/agentcommerce/domain/inventory/UpsertInventoryItemRequest.java
    title: UpsertInventoryItemRequest
  - id: item-response
    resource: ../../../domain-api/src/main/java/com/agentcommerce/domain/inventory/InventoryItemResponse.java
    title: InventoryItemResponse
---
# Access Control

Inventory management endpoints are restricted to active vendor-location managers/owners/admins or platform users with the `OPERATOR` role. Customer-facing menu reads are authenticated read-only and only return currently available items.

# Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/vendor-locations/{locationId}/inventory` | List vendor-location inventory |
| `PUT` | `/v1/vendor-locations/{locationId}/inventory/items` | Create or update one inventory item |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports` | Stage and validate CSV import |
| `GET` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}` | Read staged import result |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}/commit` | Commit valid staged rows |
| `GET` | `/v1/vendor-locations/{locationId}/menu` | Read currently available customer menu items |

# JSON Item Shape

Manual item upsert accepts and returns a single optional `imageUrl` string. It is metadata for an externally hosted product image, not an upload endpoint or object-storage workflow.

# CSV Format

Required columns:

```csv
sku,name,price,quantity_on_hand,is_available
```

Optional columns:

```csv
category,currency,reorder_threshold,image_url
```

# Future Media Upload Flow

When true uploads are implemented, vendor/admin image files SHOULD be stored in object storage, not PostgreSQL. Production storage should use Google Cloud Storage; local development should use MinIO. The database should store durable metadata such as object key, served URL, status, alt text, sort order, uploader, timestamps, and version.

Upload lifecycle:

1. Vendor-location manager/owner/admin or platform `OPERATOR` uploads an image.
2. Backend validates content type, size, dimensions, and malware/safety constraints.
3. Backend writes original and generated derivatives to object storage.
4. Backend stores image metadata and served URL in PostgreSQL.
5. Menu responses return only active image URLs.

Replace lifecycle:

1. Vendor/admin uploads the replacement image.
2. Backend stores the new object in GCS/MinIO and validates/derives served URLs.
3. Backend updates PostgreSQL in one transaction so the new image becomes the only active menu image.
4. Backend marks the prior image metadata as `REPLACED`, `INACTIVE`, or `DELETED`; it should not remain menu-visible.
5. Backend deletes the prior object-storage asset, or schedules deletion, only after the DB update succeeds.
6. Menu responses return the new URL on the next read.

Delete lifecycle:

1. Vendor/admin requests image deletion.
2. Backend removes image from menu visibility immediately by status or metadata update.
3. Backend deletes the object-storage asset or schedules deletion after successful DB update.
4. Historical orders/audit keep immutable references or snapshots where needed.

# Image Support

The current executable slice supports one optional image URL per catalog item via `imageUrl` in JSON requests/responses and `image_url` in CSV imports. For the current single-URL slice, replacing an image means updating the item to the new `imageUrl`; future object-storage-backed media should preserve lifecycle metadata while exposing only the active URL. Full image upload, object storage, moderation, galleries, alt text, CDN lifecycle, and image ordering remain future catalog/media work.

# Future Item And Price Lifecycle

Catalog item identity, availability, price, and media SHOULD remain separate concepts:

| Concern | Future responsibility |
| --- | --- |
| Catalog item | What the item is: SKU, name, description, category, modifier/variant family, lifecycle status |
| Inventory availability | Whether/how much can be sold now, including quantity, out-of-stock, and timed unavailability |
| Price | Current and scheduled price records; accepted orders keep immutable price snapshots |
| Media | One or more object-storage-backed images with metadata and lifecycle status |

Item lifecycle should prefer soft states over hard deletion:

- `ACTIVE`: visible and potentially sellable when availability allows.
- `INACTIVE`: disabled/hidden by vendor/admin.
- `DELETED`: removed from normal UI but retained for audit, imports, analytics, and historical orders.

Availability lifecycle should support restaurant-style temporary controls:

- `AVAILABLE`: sellable.
- `OUT_OF_STOCK`: temporarily unavailable, often until next prep cycle or closing time.
- `UNAVAILABLE_UNTIL`: unavailable until an explicit timestamp.
- `PAUSED_FOR_WINDOW`: unavailable for configured hours/days/months.
- `DISCONTINUED`: no longer sold, while history remains intact.

Timed availability metadata should include `unavailable_from`, `unavailable_until`, `unavailable_reason`, `auto_restore_at`, and an optional vendor/customer-facing note.

Price change lifecycle:

1. Vendor-location manager/owner/admin or platform `OPERATOR` submits a new price.
2. Backend validates authorization, currency, non-negative amount, policy limits, and expected version/effective date.
3. Backend creates a new effective price record or updates the current active price with version increment, depending on implementation stage.
4. Prior price metadata should be retained as history where possible, especially once carts, quotes, orders, promotions, or audits reference prices.
5. Menu reads return the current active price.
6. Existing accepted quotes/orders keep their original immutable price snapshot and are never changed by later catalog price updates.

# Generation Rules

- Treat CSV input as untrusted.
- Stage and validate before mutating live inventory.
- Preserve explicit commit as a separate action.
- Scope every inventory management read and mutation to an authenticated vendor-location manager/owner/admin or platform `OPERATOR`.
- Customer menu reads must expose only eligible, active, currently available items and must hide inactive, deleted, out-of-stock, or time-paused items unless a future UI explicitly supports disabled display.
