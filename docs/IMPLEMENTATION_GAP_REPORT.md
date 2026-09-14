# Implementation Gap Report

**Audit date:** 2026-09-13
**Scope:** Repository implementation compared with `SPEC.md`, `TECH_SPEC.md`, and `docs/FUNCTIONALITY_CATALOG.md`

## 1. Executive Result

The repository currently implements foundational customer identity/profile, vendor onboarding/location settings, and dasher onboarding/verification/availability APIs. The four role applications and REST portal provide responsive UI shells and limited connections to those implemented APIs.

Catalog, inventory, CSV upload, carts, quotes, orders, payments, active dispatch/delivery, messaging, offers, support, agent orchestration, and GCP deployment are not yet implemented as complete executable vertical slices.

## 2. Inventory Upload and Display Verdict

**Current status: Partial implementation.**

A vendor can now stage and commit a supported CSV through the vendor UI, retrieve authoritative inventory, and display currently available items in the customer menu. Advanced import controls, richer catalog modeling, reservations, events, agent adapters, and end-to-end tests remain.

### Evidence

- Database migrations do not create product, variant, inventory record, inventory import, or inventory staging tables.
- `V6__create_pos_integration_schema.sql` creates `inventory_sync_logs`, but that is a POS synchronization log rather than source-of-truth inventory.
- Java source packages contain only `identity`, `vendor`, `driver`, and `security`; there is no catalog or inventory module.
- No catalog, inventory, upload, import validation, commit, or rollback REST controller exists.
- Neither portal contains a file input, multipart upload, inventory fetch, inventory table, staged validation preview, or commit action.
- `portal/role-app.js` uses hard-coded vendor metrics and changes the Inventory tab to a placeholder message.

### Required End-to-End Behavior

1. Vendor selects a location and downloads or follows the supported UTF-8 CSV template.
2. UI uploads the file as multipart content with an idempotency key.
3. Backend stores the original object, parses safely, and creates an import record plus staged rows.
4. Backend returns row-level errors/warnings and an import summary without mutating live catalog/inventory.
5. UI displays a preview table, filters errors, and lets the vendor exclude invalid rows.
6. Vendor explicitly commits valid staged rows.
7. Backend transaction creates/updates products, variants, and inventory with optimistic concurrency, audit, and outbox events.
8. Vendor UI retrieves and renders committed inventory from the authoritative inventory API.
9. Customer discovery retrieves only eligible, active, currently available items through catalog/search APIs.
10. Rollback is allowed only while affected record versions remain unchanged.

## 3. Required Inventory Vertical Slice

### Database

- Product, variant, modifier, category, allergen, and location-menu tables.
- Inventory record with quantity/availability mode, on-hand, reserved, reorder threshold, status, source, timestamps, and version.
- Inventory reservation state and expiry.
- Inventory import, staged row, row issue, commit result, and rollback metadata.
- Audit/outbox records and tenant/location constraints.

### REST API

- Product and variant CRUD scoped to vendor location.
- Paginated inventory list with search and filters.
- Versioned single-item and bulk inventory updates.
- CSV template metadata/download.
- Multipart CSV stage/validate endpoint.
- Import preview and row exclusion endpoint.
- Explicit commit and conditional rollback endpoints.
- Customer-facing eligible catalog/menu endpoint.

### Vendor UI

- Location selector and inventory table populated from REST data.
- Search, category/status filters, sorting, pagination, and low-stock indicators.
- Inline quantity/availability editing with conflict handling.
- Bulk activate, deactivate, and quantity adjustment.
- Drag/drop or file-picker CSV upload.
- Staged preview with row errors/warnings and commit confirmation.
- Import history, audit ID, result summary, and eligible rollback.

### Customer UI

- Vendor menu populated from the customer catalog API.
- Availability, variants, modifiers, allergens, price, and active offer display.
- Unavailable items hidden or clearly disabled according to policy.
- Revalidation when adding to cart and again during quote/checkout.

### Testing

- Unit tests for parsing, validation, duplicate SKU handling, money, and state rules.
- Integration tests for authorization, tenant isolation, upload/stage/commit/rollback, and REST responses.
- Concurrency tests for stale updates, reservation races, and no oversell.
- Contract tests proving vendor and customer UIs consume the documented responses.
- End-to-end test: upload CSV → preview → commit → vendor inventory display → customer menu display.

## 4. Domain Implementation Status

| Domain | Current evidence | Status |
| --- | --- | --- |
| Customer identity/profile | Controllers, repositories, services, unit and integration tests | Implemented foundation |
| Vendor onboarding/location | Controllers, repositories, services, unit and integration tests | Implemented foundation |
| Dasher onboarding/verification | Controllers, repositories, services, unit and integration tests | Implemented foundation |
| Catalog/menu | Specification and schema design only | Not implemented |
| Inventory and CSV import | Specification and schema design only; POS sync log is insufficient | Not implemented |
| Customer discovery/search | No backend module or live UI query | Not implemented |
| Cart and quote | No migration, module, endpoint, or UI workflow | Not implemented |
| Orders/sub-orders/substitutions | No migration, module, endpoint, or UI workflow | Not implemented |
| Payments/ledger/transfers | Merchant account schema only; no payment service | Not implemented |
| Dispatch/delivery | Dasher profile exists; delivery offers, assignment, stops, and proof do not | Partial foundation |
| Messaging/notifications | Agent message schema exists; no executable messaging service | Schema only |
| Promotions/offers | Tables exist; no repository/service/controller or live UI | Schema only |
| Support/refunds/feedback | No executable module | Not implemented |
| Agent orchestration/tools | Agent governance tables exist; no agent service or tool endpoints | Schema only |
| Four role applications | Responsive PWA shells and limited API checks | Partial |
| GCP deployment | GCP architecture documented; no deployable Terraform/Cloud Build configuration | Not implemented |

## 5. Priority Order

1. Catalog and inventory schema plus vendor-scoped REST APIs.
2. CSV stage, validation, commit, rollback, and audit pipeline.
3. Vendor inventory UI connected to real APIs.
4. Customer menu/discovery API and UI connected to committed inventory.
5. Cart, quote, reservation, and checkout state machine.
6. Vendor order handling and substitution coordination.
7. Payment, dispatch/delivery, messaging, support, and promotions.
8. Agent orchestration over completed domain tools.
9. GCP infrastructure, observability, security, and release automation.

The inventory vertical slice should be implemented before agent inventory actions. The vendor agent and UI must call the same service so authorization, validation, concurrency, audit, and events cannot diverge.