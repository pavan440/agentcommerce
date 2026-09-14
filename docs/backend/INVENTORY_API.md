# Inventory and Catalog API

## Agent-Native Boundary

`InventoryService` is the authoritative application service for both manual UI calls and future vendor-agent tools. The agent must delegate with the vendor user's identity and call this service through a protected internal adapter; it must not write inventory tables directly.

## Implemented Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/v1/vendor-locations/{locationId}/inventory` | Vendor-scoped authoritative inventory list |
| PUT | `/v1/vendor-locations/{locationId}/inventory/items` | Create or update an item with optional version check |
| POST | `/v1/vendor-locations/{locationId}/inventory-imports` | Upload and stage a UTF-8 CSV multipart `file` |
| GET | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}` | Retrieve staged rows and validation results |
| POST | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}/commit` | Explicitly commit valid included rows |
| GET | `/v1/vendor-locations/{locationId}/menu` | Read currently available customer menu items |

CSV requires `sku,name,price,quantity_on_hand,is_available`. Supported optional fields are `category,currency,reorder_threshold`. Price is decimal currency input and is stored in minor units.

## UI Flow

Open `/vendor/`, select **Inventory**, enter a vendor location ID, choose a CSV, stage it, review row errors, and commit valid rows. The committed list is reloaded from the API. Open `/customer/`, select **Discover**, enter the same location ID, and load the available menu.

## Remaining Work

- Staged-row inclusion/exclusion mutation.
- Conditional rollback and import history listing.
- Variant, modifier, allergen, image, and category modeling.
- Bulk edit endpoints and richer pagination/filtering.
- Reservation lifecycle, outbox/audit events, object storage, and malware/CSV-formula defenses.
- Protected internal agent-tool controllers with delegated-principal tokens and approval/audit integration.
- Inventory API integration and end-to-end browser tests.