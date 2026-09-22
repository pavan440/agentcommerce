# OKF bundle log

## 2026-09-22

- Created the first OKF bundle for AgentCommerce.
- Added source-linked concepts for project overview, generation rules, architecture boundaries, domain map, inventory API status, implementation status, and gap priority.
- Marked implementation-status concepts as draft where repository sources disagree about inventory freshness.
## 2026-09-22 database and UI expansion

- Added database concepts for migration order, identity, vendor/location, merchant finance, agent governance, catalog/inventory, POS, promotions, and community schemas.
- Added UI concepts for the current PWA implementation, role workspace map, shared design system, and voice/agent UI transition.
- Updated the OKF index so future agents can navigate by project, database, API, UI, implementation, and roadmap concerns.

## 2026-09-22 inventory image URL and access-control update

- Updated inventory API and schema concepts for single `imageUrl` / `image_url` support.
- Added the V8 image URL migration to the database migration map.
- Clarified inventory management access as vendor-location manager/owner/admin or platform `OPERATOR` only.
- Clarified that menu reads are authenticated read-only and rich media upload/gallery support remains future work.

## 2026-09-22 inventory lifecycle and media storage agreement

- Added future OKF guidance for object-storage-backed image uploads with database metadata and delete/replace lifecycle.
- Added future item lifecycle guidance for active, inactive, soft-deleted, out-of-stock, unavailable-until, paused-window, and discontinued states.
- Added future price lifecycle guidance requiring versioned or effective-dated prices and immutable quote/order snapshots.
- Clarified that code changes should wait until the relevant OKF section is agreed and valid.

## 2026-09-22 inventory image replacement lifecycle

- Clarified that replacing an item image uploads the new object first, updates database metadata/active URL, retires the old metadata row, and deletes or schedules deletion of the old object after DB success.
- Clarified that menu reads should return only the active replacement image URL.

## 2026-09-22 inventory price change lifecycle

- Clarified that price changes create/update the active price for menu reads while retaining prior price history where possible.
- Clarified that accepted quotes and orders keep immutable price snapshots and are not changed by later catalog price updates.
