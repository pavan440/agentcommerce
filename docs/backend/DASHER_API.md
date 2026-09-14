# Dasher Backend

## Scope

This slice implements the initial dasher lifecycle:

1. An authenticated customer starts dasher onboarding.
2. The API creates a pending driver profile and grants the global `DRIVER` role transactionally.
3. The dasher maintains vehicle details and one or more PostGIS operating zones.
4. A platform `OPERATOR` records the background-check decision and activates or suspends the dasher.
5. A verified active dasher can go online and offline using optimistic locking.

Document upload and external background-check provider integration are not part of the current schema and remain separate workflows.

## Endpoints

All endpoints require a bearer JWT. Identity is resolved through the immutable OIDC `(issuer, subject)` pair.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/v1/dashers/me/onboarding` | Creates a pending dasher profile and `DRIVER` role |
| GET | `/v1/dashers/me` | Returns the current dasher profile |
| PATCH | `/v1/dashers/me` | Applies a versioned vehicle/profile update |
| PATCH | `/v1/dashers/me/availability` | Changes online status using `expectedVersion` |
| GET | `/v1/dashers/me/operating-zones` | Lists owned spatial operating zones |
| POST | `/v1/dashers/me/operating-zones` | Creates a PostGIS multipolygon from boundary points |
| DELETE | `/v1/dashers/me/operating-zones/{zoneId}` | Deletes an owned operating zone |
| PATCH | `/v1/operators/dashers/{dasherId}/verification` | Records an operator verification decision |

## Onboarding

Motor vehicles (`CAR` and `SCOOTER`) require make, model, color, license plate, and license number. `BICYCLE` and `FOOT` profiles do not require motor-vehicle data. Changing to either non-motorized type clears stored motor-vehicle fields.

New profiles start with:

- `status = PENDING_VERIFICATION`;
- `backgroundCheckStatus = PENDING`;
- `online = false`;
- `averageRating = 5.00`;
- `totalDeliveries = 0`.

The API never returns the full driver's license number. Responses expose only `licenseNumberLast4`.

## Operating Zones

A zone request supplies a name and between 3 and 1,000 latitude/longitude boundary points:

```json
{
  "zoneName": "Downtown Seattle",
  "boundary": [
    {"latitude": 47.6200, "longitude": -122.3500},
    {"latitude": 47.6200, "longitude": -122.3200},
    {"latitude": 47.5900, "longitude": -122.3200},
    {"latitude": 47.5900, "longitude": -122.3500}
  ]
}
```

The service requires at least three distinct points, closes the ring when needed, and stores the result as `GEOGRAPHY(MULTIPOLYGON, 4326)`. Responses expose GeoJSON. An online dasher cannot delete their final operating zone.

## Verification and Availability

Only a local user with the `OPERATOR` role can call the verification endpoint. Activating a dasher requires `backgroundCheckStatus = APPROVED`. Suspension forces the dasher offline.

Going online requires all of the following:

- profile status is `ACTIVE`;
- background check is `APPROVED`;
- at least one operating zone exists;
- `expectedVersion` matches the stored profile version.

Every profile, verification, and availability mutation increments `version`. Stale writes return `409 Conflict` and clients must reload before retrying.

## Ownership and Errors

Zone reads and deletes derive ownership from the authenticated local user. Another user's zone identifier returns `404` rather than revealing ownership.

Errors use RFC 9457 problem details:

- `400` for invalid vehicle data, boundaries, or state combinations;
- `403` for unverified availability changes or missing operator authority;
- `404` for missing/non-owned profiles and zones;
- `409` for duplicate onboarding and stale versions.

## Tests

`DasherServiceTest` covers duplicate onboarding, motor-vehicle requirements, stale updates, online eligibility, operator authority, verification rules, and zone ownership.

`DasherApiIntegrationTest` runs the full HTTP, JWT security, identity provisioning, role assignment, Flyway, PostgreSQL, PostGIS, and optimistic-lock path. It is gated by `RUN_DB_INTEGRATION_TESTS=true`.