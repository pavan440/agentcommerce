# Vendor Onboarding Backend

## Scope

This backend slice implements the first deterministic vendor workflow:

1. An authenticated OIDC principal is provisioned through the identity module.
2. The local user creates a draft vendor and becomes its active owner.
3. An owner or administrator creates draft physical locations.
4. Authorized location managers read and update commerce settings.
5. Commerce settings use optimistic locking to reject stale writes.

New vendors and locations start in DRAFT. Activation and verification are separate workflows.

## Identity and Authorization

JWT (issuer, subject) values are mapped through CustomerIdentityService. Controllers never treat the external JWT subject as a database UUID.

Creating a vendor transactionally creates:

- the vendors row;
- the user's global VENDOR_MEMBER role, if absent;
- an active OWNER membership.

Location creation requires an active OWNER or ADMIN membership for the vendor.

Commerce settings require one of:

- an active OWNER membership;
- an active ADMIN membership;
- an active MANAGER membership explicitly scoped to the location through vendor_membership_locations.

Authorization failures return 403 Forbidden.

## Endpoints

All endpoints require a bearer JWT.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | /v1/vendors | Creates a draft vendor and owner membership |
| GET | /v1/vendors/me | Lists vendors for the current local user |
| GET | /v1/vendors/{vendorId} | Returns a vendor or 404 |
| POST | /v1/vendors/{vendorId}/locations | Creates a draft location for an authorized owner/admin |
| GET | /v1/vendor-locations/{locationId} | Returns a location or 404 |
| GET | /v1/vendor-locations?communityCode=... | Lists active locations in a normalized community code |
| GET | /v1/vendor-locations/{locationId}/commerce-settings | Returns settings for an authorized manager |
| PATCH | /v1/vendor-locations/{locationId}/commerce-settings | Applies an authorized versioned update |

## Validation and Errors

Create requests validate required names, canonical slugs, field sizes, email formats, country/currency codes, and geographic coordinate ranges.

Commerce settings validate non-negative amounts, positive preparation times, supported enum values, and preparation-time ordering.

Errors use RFC 9457 problem details:

- 400 for request validation and invalid domain arguments;
- 403 for missing vendor/location authority;
- 404 for missing read resources;
- 409 for duplicate resources and stale expectedVersion values.

A client updating commerce settings must send the current expectedVersion. A successful update increments the stored version. A stale update returns 409 and the client must reload before retrying.

## Tests

VendorServiceTest covers authorization guards, preparation-range rules, community validation, and optimistic-lock conflicts without infrastructure.

VendorApiIntegrationTest runs the full HTTP, Spring Security, identity provisioning, transaction, Flyway, PostgreSQL, PostGIS, and pgvector path. Compose builds a custom PostgreSQL image containing both required extensions and starts an isolated tmpfs-backed postgres-test service. It is gated by RUN_DB_INTEGRATION_TESTS=true so normal unit tests do not require a database.

The integration workflow verifies:

- draft vendor creation and normalized fields;
- transactional owner membership creation;
- authorized draft location creation;
- default commerce settings;
- successful versioned settings update;
- stale update conflict;
- cross-user authorization denial;
- invalid payload rejection.
