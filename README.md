# AgentCommerce

AgentCommerce is an agent-native community delivery marketplace for customers, vendors, dashers, and platform operators. Each role has a personal AI agent that can recommend and execute approved actions through the same secured REST services used by the manual applications.

The project targets Google Cloud Platform using a community-cell architecture. Transactional commerce remains deterministic and auditable: agents do not write directly to databases or bypass authorization, approval, inventory, payment, or workflow rules.

## Current Status

Implemented foundations:

- OIDC/JWT-secured Spring Boot domain API.
- Customer profile, addresses, consent, devices, and emergency contacts.
- Vendor onboarding, memberships, locations, and commerce settings.
- Dasher onboarding, verification, zones, and availability.
- Catalog and inventory records with vendor-location tenancy.
- Staged UTF-8 CSV inventory validation and explicit commit.
- Customer-facing available-menu API.
- Four responsive installable role PWAs.
- PostgreSQL migrations with PostGIS and pgvector support.
- Unit and PostgreSQL-backed integration testing.

Still under development:

- Rich catalog variants, modifiers, allergens, images, and semantic search.
- Import row exclusion, rollback, object storage, and import history.
- Inventory reservations and outbox events.
- Cart, quote, order, payment, dispatch, messaging, support, and offer services.
- Protected internal agent-tool adapters and the agent orchestration service.
- Production GCP Terraform, Cloud Build, observability, and release automation.

See `docs/IMPLEMENTATION_GAP_REPORT.md` for the detailed gap assessment.

## Applications

After starting the stack, open:

| Application | URL | Purpose |
| --- | --- | --- |
| Customer | `http://localhost:3000/customer/` | Menu discovery and customer-agent workspace |
| Vendor | `http://localhost:3000/vendor/` | Orders, inventory upload, offers, inquiries, and vendor-agent workspace |
| Dasher | `http://localhost:3000/dasher/` | Availability, offers, routes, and earnings workspace |
| Admin | `http://localhost:3000/admin/` | Operations, verification, support, safety, and platform health |
| REST Lab | `http://localhost:3000/` | Direct API inspection and implemented workflow testing |
| Domain API | `http://localhost:8080` | Secured Spring Boot REST API |

The role applications currently run as responsive PWAs on Android, iOS, tablets, and desktop browsers. Native App Store and Play Store packaging is a future Expo/React Native release task.

## Architecture

```text
Customer / Vendor / Dasher / Admin apps
                  |
             OIDC + HTTPS
                  |
        Spring Boot Domain API
         |        |        |
   PostgreSQL   events   providers
         ^
         |
Future role-agent tool adapters
         ^
         |
Customer / Vendor / Dasher agents
```

Core rule: user interfaces and agents call the same application services. This keeps authorization, validation, optimistic concurrency, approvals, audit, and business state consistent.

Production architecture:

- Cloud Load Balancing and Cloud Armor.
- Cloud Run for domain and agent services.
- Cloud SQL for PostgreSQL 16 with PostGIS and pgvector.
- Memorystore for Redis.
- Cloud Pub/Sub for durable events.
- Google Cloud Storage for uploads and evidence.
- Secret Manager and Artifact Registry.

## Prerequisites

- Docker Desktop with Docker Compose.
- Git.
- A configured OIDC provider and valid bearer JWT for secured API workflows.

Java and Maven do not need to be installed locally when using the provided Docker build.

## Local Development

Build and start the services:

```powershell
docker compose up --build -d
```

Check running services:

```powershell
docker compose ps
```

View API logs:

```powershell
docker compose logs -f domain-api
```

Stop the stack:

```powershell
docker compose down
```

The repository may contain an existing local PostgreSQL volume with older Flyway checksums. Preserve it unless its data is disposable. For clean migration and integration validation, use the `postgres-test` service described below.

## Authentication

The API validates real OIDC bearer JWTs. The applications do not provide development bypass headers or role-escalation backdoors.

In the browser:

1. Open a role application or the REST Lab.
2. Enter a bearer JWT issued by the configured OIDC tenant.
3. Use a token whose role and memberships match the requested resource.

Tokens are retained only in browser `sessionStorage` by the role applications.

## Inventory Workflow

### CSV Format

Required columns:

```csv
sku,name,price,quantity_on_hand,is_available
```

Supported optional columns:

```csv
category,currency,reorder_threshold
```

Example:

```csv
sku,name,price,quantity_on_hand,is_available,category,currency,reorder_threshold
TFN-001,Daily Vegetarian Tiffin,12.50,30,true,Meal Plans,USD,5
SNK-010,Samosa Plate,6.99,20,true,Snacks,USD,4
BRY-020,Chicken Biryani,15.49,0,false,Entrees,USD,6
```

### Vendor UI

1. Open `http://localhost:3000/vendor/`.
2. Enter a valid vendor JWT.
3. Select **Inventory**.
4. Enter a vendor location UUID.
5. Choose a CSV file and select **Stage CSV**.
6. Review valid and invalid rows.
7. Select **Commit valid rows**.
8. Select **Load inventory** to retrieve committed records from the API.

### Customer UI

1. Open `http://localhost:3000/customer/`.
2. Enter a valid customer JWT.
3. Select **Discover**.
4. Enter the vendor location UUID.
5. Select **Show menu**.

Only active and currently available inventory is returned by the menu endpoint.

## Inventory API

| Method | Endpoint |
| --- | --- |
| `GET` | `/v1/vendor-locations/{locationId}/inventory` |
| `PUT` | `/v1/vendor-locations/{locationId}/inventory/items` |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports` |
| `GET` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}` |
| `POST` | `/v1/vendor-locations/{locationId}/inventory-imports/{importId}/commit` |
| `GET` | `/v1/vendor-locations/{locationId}/menu` |

See `docs/backend/INVENTORY_API.md` for details.

## Testing

Build the backend and run its unit tests:

```powershell
docker compose build domain-api
```

Start a fresh integration database:

```powershell
docker compose --profile test up -d postgres-test
```

Run the complete database-enabled suite:

```powershell
docker run --rm --network agentcommerce_default `
  -v "${PWD}/domain-api:/workspace" `
  -w /workspace `
  -e RUN_DB_INTEGRATION_TESTS=true `
  -e DB_URL=jdbc:postgresql://postgres-test:5432/agentcommerce_test `
  -e DB_USER=agentcommerce `
  -e DB_PASSWORD=agentcommerce_test `
  maven:3.9-eclipse-temurin-21 mvn -B test
```

Validate portal JavaScript and Compose:

```powershell
node --check portal/app.js
node --check portal/role-app.js
node --check portal/sw.js
docker compose config --quiet
```

## Repository Layout

```text
domain-api/        Spring Boot domain API, migrations, and tests
portal/            REST Lab and four responsive role applications
docs/backend/      Implemented API documentation
docs/database/     Database design specifications
docs/architecture/ Agent-native and GCP architecture specifications
docs/ui/           UI, PWA, and voice-agent design
docs/okf/          Open Knowledge Format bundle for agent-readable project context
SPEC.md             Product requirements
TECH_SPEC.md        Technical implementation contract
```

## Key Documentation

- `docs/okf/` - OKF bundle that agents should read first for code generation context.
- `SPEC.md` — product requirements and role-agent behavior.
- `TECH_SPEC.md` — domain boundaries, APIs, events, security, testing, and GCP deployment.
- `docs/FUNCTIONALITY_CATALOG.md` — traceable functionality and audit status.
- `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` — customer/vendor agent operating model.
- `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` — agent-to-agent routing and approval rules.
- `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` — production cloud architecture.
- `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` — four-application UI and voice roadmap.
- `docs/IMPLEMENTATION_GAP_REPORT.md` — implemented and missing vertical slices.

## Agent Safety Model

- Every action is delegated by an authenticated principal or authorized platform workflow.
- Agent tools are role-scoped and least-privilege.
- Consequential actions require current policy coverage or explicit approval.
- Financial, allergen, timing, and fulfillment impacts are shown before confirmation.
- Agent-to-agent communication uses structured APIs and events.
- Free-form model messages cannot directly commit business state.
- Actions, approvals, denials, tool calls, and results are correlated and auditable.
- Manual workflows remain available when agent services are unavailable.

## License

No open-source license has been declared for this repository.
