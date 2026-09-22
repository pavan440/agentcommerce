---
type: API Surface
title: Inventory and customer menu API
description: Current API slice for vendor inventory import, inventory management, and customer menu display.
tags: [api, inventory, catalog, customer-menu, vendor]
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
---
# Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/vendor-locations/{locationId}/inventory` | List vendor-location inventory |
| `PUT` | `/v1/vendor-locations/{locationId}/inventory/items` | Create or update one inventory item |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports` | Stage and validate CSV import |
| `GET` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}` | Read staged import result |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}/commit` | Commit valid staged rows |
| `GET` | `/v1/vendor-locations/{locationId}/menu` | Read currently available customer menu items |

# CSV Format

Required columns:

```csv
sku,name,price,quantity_on_hand,is_available
```

Optional columns:

```csv
category,currency,reorder_threshold
```

# Generation Rules

- Treat CSV input as untrusted.
- Stage and validate before mutating live inventory.
- Preserve explicit commit as a separate action.
- Scope every read and mutation to the authenticated vendor/location context.
- Customer menu reads must expose only eligible, active, currently available items.
