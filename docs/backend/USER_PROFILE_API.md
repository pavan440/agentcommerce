# Customer User Backend

## Scope

This slice implements authenticated customer self-service for profiles, delivery addresses, device tokens, emergency contacts, and consent history. Driver onboarding is intentionally separate because it requires document verification and operator approval policies.

All routes resolve the bearer JWT `(issuer, subject)` through the identity module. External subjects are never treated as database UUIDs. First access provisions the local user, `CUSTOMER` role, and customer profile.

## Endpoints

All endpoints require a bearer JWT.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/v1/me` | Returns the current local customer profile and roles |
| PATCH | `/v1/me` | Applies a versioned partial profile update |
| GET | `/v1/me/addresses` | Lists owned addresses, default first |
| POST | `/v1/me/addresses` | Creates an owned address |
| DELETE | `/v1/me/addresses/{addressId}` | Deletes an owned address |
| GET | `/v1/me/device-tokens` | Lists token metadata without exposing raw tokens |
| POST | `/v1/me/device-tokens` | Registers or refreshes a token for this user |
| DELETE | `/v1/me/device-tokens/{tokenId}` | Deletes an owned token |
| GET | `/v1/me/emergency-contacts` | Lists owned emergency contacts |
| POST | `/v1/me/emergency-contacts` | Creates an emergency contact |
| DELETE | `/v1/me/emergency-contacts/{contactId}` | Deletes an owned contact |
| GET | `/v1/me/consents` | Returns the latest decision for each consent type |
| POST | `/v1/me/consents/{type}` | Appends a consent grant or revocation |

Supported consent types are `LOCATION_TRACKING` and `AGENT_PERSONALIZATION`. Consent path values are case-insensitive.

## Profile Updates

`PATCH /v1/me` accepts any mutable profile fields plus the required `expectedVersion`:

```json
{
  "displayName": "Ada Customer",
  "timezone": "America/Los_Angeles",
  "dietaryRestrictions": ["VEGETARIAN"],
  "allergens": ["PEANUTS"],
  "substitutionMode": "PREFERENCE_BASED",
  "autoOrderEnabled": true,
  "autoOrderMaxAmountMinor": 7500,
  "defaultTipPercentage": 18.50,
  "expectedVersion": 0
}
```

A successful update increments `version`. A stale `expectedVersion` returns `409 Conflict`; clients must reload before retrying. Enabling auto-order requires a positive maximum amount.

## Resource Rules

- The first address becomes the default even when `isDefault` is false.
- Creating a new default address clears the previous default atomically.
- Deleting the default promotes the oldest remaining address.
- Address, token, and contact deletes include the current user in the database predicate; another user's identifier returns `404`.
- A raw device token can belong to only one user. Cross-user registration returns `409`.
- Device-token responses omit the raw token.
- Consent changes are append-only; reads select the latest row per type.

## Validation and Errors

Requests validate field sizes, enum values, monetary ranges, geographic coordinates, platform values, and E.164-style emergency phone numbers.

Errors use RFC 9457 problem details:

- `400` for invalid requests or unsupported consent types;
- `404` for missing or non-owned resources;
- `409` for stale profile versions or token ownership conflicts.

## Tests

`UserProfileServiceTest` covers optimistic-lock conflicts, auto-order safety, ownership-safe deletes, device-token conflicts, and consent normalization.

`UserApiIntegrationTest` runs through Spring MVC, JWT security, identity provisioning, Flyway, PostgreSQL, PostGIS, and transaction boundaries. It verifies profile updates, stale writes, default-address invariants, ownership isolation, token secrecy, emergency contacts, and consent history. The integration suite is gated by `RUN_DB_INTEGRATION_TESTS=true`.