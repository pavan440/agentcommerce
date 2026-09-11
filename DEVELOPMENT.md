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

- Domain API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

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
docker run --rm -v "${PWD}/domain-api:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn test
```

