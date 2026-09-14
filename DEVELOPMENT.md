# Development Guide

## Prerequisites

- Docker Desktop with Docker Compose
- An OpenID Connect development tenant for interactive customer login

Java and Maven do not need to be installed locally. The backend image compiles the application with Java 21 and Maven.

## Start the Foundation

```powershell
docker compose up --build
```

The services are available at:

- Marketplace portal: `http://localhost:3000`
- Domain API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

The portal workflow and bearer-token setup are documented in `docs/ui/MARKETPLACE_PORTAL.md`.

The default OIDC URLs point to a future local identity provider. For a managed development tenant, set these before starting:

```powershell
$env:OIDC_ISSUER_URI='https://issuer.example.com/'
$env:OIDC_JWK_SET_URI='https://issuer.example.com/.well-known/jwks.json'
docker compose up --build
```

## Customer Identity Endpoint

`GET /v1/me` requires a bearer JWT. On first access, the API creates a local customer record keyed by the immutable OIDC `(issuer, subject)` pair. Later requests return the same customer.

The database stores platform roles and profile data, but never customer passwords or access tokens.

## Run Tests

```powershell
docker run --rm -v "${PWD}/domain-api:/workspace" -v agentcommerce-maven-cache:/root/.m2 -w /workspace maven:3.9.11-eclipse-temurin-21 mvn test
```


## Customer User Backend

Customer profile, address, device-token, emergency-contact, and consent APIs are documented in `docs/backend/USER_PROFILE_API.md`.

Mutations derive ownership from the authenticated OIDC principal. Profile writes use optimistic locking, address writes maintain one default, and device-token responses never expose the raw token.

## Dasher Backend

Dasher onboarding, vehicle, operating-zone, operator-verification, and availability APIs are documented in `docs/backend/DASHER_API.md`.

Dasher writes resolve the JWT to a local user, mask license numbers in responses, enforce PostGIS zone ownership, and use optimistic locking for profile state changes.

## Vendor Backend

The initial vendor onboarding implementation and API contract are documented in `docs/backend/VENDOR_ONBOARDING_API.md`.

Vendor mutations resolve the authenticated OIDC principal to a local user. Location and commerce-settings mutations enforce active vendor membership and never trust a raw JWT subject as a database identifier.

## Run Database Integration Tests

Start only the PostGIS dependency:

~~~powershell
docker compose --profile test up --build -d postgres-test
~~~

Run the full test suite in Maven's Java 21 container with the integration-test gate enabled:

~~~powershell
docker run --rm --network agentcommerce_default -e RUN_DB_INTEGRATION_TESTS=true -e DB_URL=jdbc:postgresql://postgres-test:5432/agentcommerce_test -e DB_USER=agentcommerce -e DB_PASSWORD=agentcommerce_test -v "$PWD/domain-api:/workspace" -v agentcommerce-maven-cache:/root/.m2 -w /workspace maven:3.9.11-eclipse-temurin-21 mvn test
~~~

The test database uses tmpfs and is isolated from development data. Without RUN_DB_INTEGRATION_TESTS=true, Maven runs the unit suite and skips database integration tests.
