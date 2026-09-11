# Technical Specification: Agentic Community Delivery Marketplace

**Phase:** MVP Phase 1 — Marketplace Foundation  
**Version:** 1.1  
**Status:** Draft for engineering and stakeholder review  
**Product requirements source:** `SPEC.md`  
**Last updated:** 2026-09-11

## 1. Purpose and Scope

This document defines an implementation architecture for Phase 1 of the Agentic Community Delivery Marketplace. It translates the product behavior in `SPEC.md` into component boundaries, data ownership, APIs, events, state transitions, security controls, deployment requirements, and verification criteria.

Phase 1 supports:

- Customer, vendor, driver, and operator experiences.
- Role-specific conversational agents with deterministic tool execution.
- Local catalog discovery and hybrid search.
- Multi-vendor carts with one parent order and vendor-specific sub-orders.
- Inventory forms, staged CSV import, reservations, and concurrency control.
- Payment authorization, capture/adjustment, refunds, and vendor transfers through Stripe.
- One driver collecting accepted sub-orders from multiple vendors for one customer delivery.
- Messaging, notifications, support cases, and manual vendor announcements/offers.
- Auditable approvals, agent actions, and manual operation when AI is unavailable.

Phase 2 POS integrations and sales intelligence are excluded. Interfaces may reserve extension points, but Phase 2 infrastructure is not a Phase 1 dependency.

## 2. Architecture Decisions

### 2.1 System Shape

The MVP uses:

1. Client applications for customer, vendor, driver, and operator workflows.
2. A Spring Boot modular monolith that owns deterministic business state and public APIs.
3. A separately deployed Python agent orchestration service that calls authenticated domain APIs and has no transactional database credentials.
4. Managed PostgreSQL, Redis, object storage, and a durable event transport.
5. Third-party adapters for payment, routing, messaging, identity verification, push delivery, and model inference.

The modular monolith is selected to preserve local ACID transactions across inventory, quote, and order boundaries while the domain is evolving. Modules communicate through explicit application interfaces and domain events. A module may be extracted later without changing external contracts.

### 2.2 Mandatory Boundaries

- PostgreSQL is the source of truth for transactional commerce state.
- The LLM never writes directly to PostgreSQL, Redis, the event bus, or payment providers.
- All agent tool calls use the same authorization and policy services as manual UI actions.
- Structured APIs are authoritative for price, inventory, hours, order, payment, and delivery state.
- Retrieved documents and user/vendor content are untrusted data, never executable instructions.
- Financial records and accepted quote components are immutable snapshots.
- Critical asynchronous effects use a transactional outbox and durable delivery.

### 2.3 Technology Baseline

| Layer | Baseline | Notes |
| --- | --- | --- |
| Customer/driver clients | React Native with Expo | iOS and Android; responsive web may share compatible components |
| Vendor/operator clients | TypeScript web application | Tablet-friendly vendor order view and keyboard-complete operator workflows |
| Domain API | Java 21, Spring Boot 4 | Modular monolith, REST, validation, transactions, scheduled jobs |
| Agent orchestration | Python, FastAPI, LangGraph | Role graphs, tool clients, approval pauses, response streaming |
| Primary data | PostgreSQL 16 | Transactional records, JSONB where bounded, pgvector, PostGIS |
| Cache | Redis | Rate limits, short-lived cache, presence; never sole source of truth |
| Objects | S3; MinIO locally | CSVs, receipts, support and delivery evidence |
| Events | Transactional outbox to EventBridge/SQS | At-least-once delivery with idempotent consumers |
| Payments | Stripe PaymentIntents and Connect | SPT/UCP when available; PaymentMethod fallback behind one interface |
| Runtime | ECS Fargate | Private networking, autoscaling, health checks |
| Infrastructure | Terraform | Separate development, staging, and production environments |

Specific vendors and model IDs are deployment configuration, not embedded application constants.

## 3. Context and Trust Boundaries

```text
Customer / Vendor / Driver / Operator clients
                    |
              HTTPS + OAuth2
                    v
          Domain API / Edge boundary
          |          |             |
          |          |             +--> Provider adapters
          |          |                  payment/maps/message/identity
          |          v
          |    Agent orchestrator --> Model provider
          |          |
          |          +--> scoped internal domain tools
          v
 PostgreSQL + outbox --> durable events --> workers/notifications/search
          |
       S3 / Redis
```

Public clients cannot access internal tool routes. The agent service uses workload identity with short-lived credentials and scopes identifying the acting user, role, tenant, conversation, and requested tool. Provider webhooks terminate at dedicated authenticated endpoints and are reconciled asynchronously.

## 4. Domain Modules and Ownership

| Module | Owns | Primary responsibilities |
| --- | --- | --- |
| Identity & Access | users, roles, memberships, consent | Authentication integration, RBAC/ABAC, vendor-location membership, stronger verification flags |
| Customer Profile | profiles, addresses, preferences | Dietary and substitution preferences, agent policies, consented memory settings |
| Vendor & Location | vendors, locations, hours, zones | Store configuration, operating state, preparation defaults |
| Catalog & Search | products, variants, modifiers, search projection | Exact and semantic search with live eligibility filters |
| Inventory | inventory, reservations, imports | Availability, atomic reservations, expiry, CSV staging/commit/rollback |
| Cart & Quote | carts, cart items, quotes | Vendor grouping, eligibility, immutable price calculation and expiry |
| Order | parent orders, vendor orders, order items | Submission saga, guarded state transitions, substitutions, cancellation |
| Payment | payments, ledger entries, refunds, transfers | Provider abstraction, webhook reconciliation, money-state idempotency |
| Dispatch & Delivery | drivers, offers, deliveries, stops | Eligibility, route feasibility, atomic assignment, pickup/delivery proof |
| Messaging & Notification | conversations, messages, deliveries | Masked communication, templates, push/SMS/email delivery status |
| Announcement & Offer | announcements, offers | Vendor-authored Phase 1 publishing and scheduling |
| Support & Refund | cases, evidence, decisions | Rules-based resolution and operator escalation |
| Agent Governance | memory, approvals, actions, policies | Role tools, consent, action audit, emergency disable |
| Audit & Analytics | audit entries, operational projections | Append-only evidence, metrics, reporting projections |

Each module owns its tables and exposes application interfaces. Cross-module direct table writes are prohibited.

## 5. Identity, Authorization, and Tenancy

### 5.1 Authentication

- Support email or phone authentication through an OpenID Connect provider.
- Access tokens are short-lived JWTs validated for issuer, audience, signature, expiry, and scopes.
- Refresh credentials use secure platform storage and are never exposed to the agent service.
- Vendor administrators and drivers require provider-backed verification before privileged or delivery actions.

### 5.2 Authorization

Authorization combines role, resource ownership, vendor-location membership, account status, workflow state, and explicit consent. Every query is tenant-scoped in the repository layer; controller-only checks are insufficient.

Roles:

- `CUSTOMER`: own profiles, carts, orders, conversations, and support cases.
- `VENDOR_MEMBER`: assigned vendor locations and their catalog, inventory, and vendor orders.
- `DRIVER`: own profile, offers, active deliveries, and delivery evidence.
- `OPERATOR`: policy-scoped support and administration with elevated-action logging.
- `AGENT_WORKLOAD`: only named internal tools, always delegated for an authenticated principal or platform workflow.

### 5.3 Consent and Privacy

Location tracking and agent personalization require separate explicit consent records with policy version and timestamp. Revocation stops future collection immediately. Users can view, correct, export, and delete eligible stored memory and account data. Legal or financial retention exceptions are reported rather than silently ignoring deletion.

## 6. Core Data Model

All mutable aggregates use UUIDv7 identifiers, `created_at`, `updated_at`, and a numeric `version`. Monetary values use integer minor units plus ISO 4217 currency. Timestamps use UTC; display localization occurs at clients.

### 6.1 Commerce Entities

- `fee_configuration`: market/category/agreement scope, basis points value for `X`, effective interval, status, change actor.
- `product_variant`: vendor location, SKU, base price, currency, status, modifier rules.
- `inventory_record`: variant, quantity on hand, quantity reserved, availability mode/status, reorder threshold, version.
- `inventory_reservation`: cart/order, variant, quantity, status, expiry, idempotency key.
- `cart`: customer, address candidate, status, version.
- `quote`: cart version, expiry, route constraints, fee configuration IDs, vendor totals, tax, delivery fee, tip, discount, final total.
- `order`: customer, quote snapshot, address snapshot, status, currency, correlation ID.
- `vendor_order`: parent order, vendor location, subtotal, status, preparation estimate.
- `order_item`: product text/modifier snapshot, base unit price, applied `X`, platform fee, tax, final unit price, quantity, substitution state.

### 6.2 Payment and Delivery Entities

- `payment`: order, provider intent, authorization/capture/refund amounts, status, idempotency references.
- `ledger_entry`: order/vendor/payment reference, account type, amount, currency, entry type, immutable timestamp.
- `transfer`: vendor order, connected account, amount, provider reference, status.
- `delivery`: order, driver, route summary, payout snapshot, state, assignment version.
- `delivery_stop`: delivery, vendor order, sequence, state, verification metadata, timestamps.
- `delivery_offer`: delivery candidate, driver, payout, route snapshot, expiry, response.

### 6.3 Agent and Operations Entities

- `agent_approval`: principal, action, canonical argument hash, financial impact, expiry, status, confirmation method.
- `agent_action`: principal, role, conversation, tool, redacted argument summary, argument hash, approval, result code, correlation ID.
- `agent_memory`: owner, role, memory type/content, source, consent version, lifecycle timestamps.
- `inventory_import`: object key, content hash, status, summary, submitter, commit/rollback metadata.
- `support_case`: reporter, order/delivery, issue type, description, evidence, risk flags, resolution, financial outcome.
- `outbox_event`: event ID, aggregate/type/version, payload, correlation/causation IDs, publish state.

### 6.4 Database Constraints

- SKU is unique per vendor location.
- A provider payment object maps to at most one internal payment.
- One active driver assignment exists per delivery.
- Event IDs and mutation idempotency keys are unique within their defined scope.
- Quantities and financial amounts cannot be negative except explicit signed ledger entries.
- `quantity_reserved <= quantity_on_hand` for quantity-tracked stock.
- Financial snapshots and ledger entries are append-only after order submission.

## 7. Pricing, Cart, and Quote Rules

The active platform fee is stored as basis points to avoid floating-point arithmetic. For each eligible item:

```text
platform_fee = round_currency(base_price * fee_basis_points / 10_000)
customer_item_price = base_price + platform_fee
```

Rounding uses the currency's minor unit and one documented half-up policy. The quote service calculates and stores base totals, platform fees, delivery fee, tax, tip, discounts, vendor totals, and final total. It also snapshots fee configuration, tax inputs, route assumptions, and quote expiry.

Any item, modifier, quantity, address, tip, discount, vendor availability, material route, or fee change invalidates the current quote. Checkout accepts only an unexpired quote for the current cart version. Changes to `X` affect new quotes only.

Configurable constraints include maximum vendor count, pickup radius, route duration, product freshness, minimum order, quote lifetime, and substitution tolerance. Missing configuration fails closed and prevents checkout.

## 8. Inventory and CSV Import

### 8.1 Reservation Algorithm

For quantity-tracked inventory, each reservation performs an atomic conditional update in one database transaction:

```sql
UPDATE inventory_record
SET quantity_reserved = quantity_reserved + :requested,
    version = version + 1
WHERE id = :inventory_id
  AND is_available = TRUE
  AND quantity_on_hand - quantity_reserved >= :requested;
```

Zero affected rows means insufficient or changed inventory. All items for one vendor sub-order reserve atomically. The order saga coordinates vendor-level transactions and releases already-created reservations if any required reservation fails.

Reservations have explicit expiry and states `active`, `committed`, `released`, or `expired`. A scheduled worker expires stale reservations idempotently. Vendor acceptance commits reservation quantity by decreasing both on-hand and reserved quantities in one transaction. Rejection, failed checkout, and eligible cancellation release it.

### 8.2 Availability-Only Inventory

Prepared food may use availability-only mode. Checkout still records a reservation marker and revalidates availability and vendor status before submission, but does not decrement a quantity.

### 8.3 CSV Workflow

1. Client requests a presigned upload URL.
2. Upload completion creates an import with object content hash and `uploaded` state.
3. A worker parses UTF-8 CSV in a restricted environment with file-size, row-count, field-length, and formula-injection controls.
4. Required and optional columns follow `SPEC.md`; unknown columns produce warnings.
5. Validated rows are stored in staging, never live inventory.
6. Vendor reviews creates, updates, deactivations, warnings, errors, and excluded invalid rows.
7. Commit requires explicit approval and an unchanged target version for every affected record.
8. One transaction applies the valid staged rows and records audit/outbox entries.
9. Rollback is permitted only if affected records retain the import's committed versions.

## 9. Order State and Submission Saga

The Order module is the sole authority for state transitions. Every transition records actor, source, timestamp, reason, previous/new state, entity version, and correlation ID.

Parent order states and vendor sub-order states are exactly those defined in `SPEC.md`. Transition commands are guarded by current state and prerequisites. Repeating the same command with the same idempotency key returns the original result. A stale or invalid transition returns `409 STATE_TRANSITION_REJECTED` without side effects.

### 9.1 Checkout Saga

1. Validate authentication, ownership, idempotency key, cart version, quote expiry, and approval.
2. Revalidate vendor hours, item/modifier status, inventory, address, taxes, and route constraints.
3. Create pending parent and vendor orders from immutable quote snapshots.
4. Reserve inventory and transition parent to `inventory_reserved`.
5. Create/confirm payment authorization for the maximum approved total.
6. On success, transition to `payment_authorized`, then submit vendor orders.
7. On failure, release reservations and mark the order canceled; retain diagnostic and payment records.
8. Vendors independently accept or reject their own sub-orders.
9. Apply the configured partial-rejection policy, recalculate capture amount, and release rejected inventory.
10. Capture at the configured policy point and create transfer ledger entries for accepted vendor orders.

No network call occurs while a database transaction is open. Saga steps persist durable intent before provider calls and reconcile ambiguous outcomes by provider object ID.

### 9.2 Substitutions

Substitution proposals reference the original item, candidate variant, price delta, expiry, and vendor order. The deterministic policy service decides whether saved preferences and tolerance allow automatic acceptance. Approval-required substitutions cannot proceed without a matching unexpired customer approval. Payment amount changes must remain within the authorized ceiling or obtain new confirmation/authorization.

## 10. Payments and Settlement

The Payment module exposes one provider-neutral interface for quote authorization, capture, cancellation, refund, and transfer. It supports Stripe Shared Payment Tokens when available and PaymentMethods otherwise.

- SPTs are created only after customer confirmation and scoped by seller, maximum amount, currency, and expiration.
- Raw card data never enters platform systems.
- PaymentIntent creation, confirmation, capture, cancellation, refund, and transfer calls use stable idempotency keys.
- Provider webhooks are signature-verified, stored by provider event ID, acknowledged quickly, and processed idempotently.
- API responses do not override a later authoritative provider webhook; reconciliation repairs mismatches.
- Separate charges and transfers allocate vendor base-price proceeds according to immutable order snapshots.
- Transfers occur only after successful capture and the configured settlement gate.
- Refunds create immutable ledger entries and adjust eligible unsettled transfers or initiate reversals.
- The platform fee, delivery fee, tip, tax, discount funding, vendor proceeds, driver payout, refund, and provider fee remain separately traceable.

Automated refund authority is configuration by issue type and amount. Cases beyond limits or with risk flags require operator approval.

## 11. Dispatch and Delivery

Dispatch begins when accepted vendor orders satisfy the configured readiness window and coordinated route constraints.

Eligibility filters include driver verification/status, online presence, service zone, vehicle/capacity, pickup route, timing, and safety rules. PostGIS handles zone and radius predicates; the selected maps provider supplies route time and order.

### 11.1 Offer and Assignment

- An offer snapshots all pickup stops, sequence, route estimate, constraints, payout, and expiry.
- Drivers can accept only unexpired offers addressed to them.
- Assignment uses a conditional update on an unassigned delivery; only one acceptance succeeds.
- Competing or repeated accept calls return the existing assignment state without duplicate effects.
- Timeout, decline, or approved unassignment creates a new offer round and preserves history.

### 11.2 Fulfillment Evidence

Each pickup requires driver arrival and vendor handoff verification before the stop becomes `collected`. Delivery cannot become `out_for_delivery` until every accepted stop is collected or a deterministic exception removes it. Final delivery requires configured evidence such as a code, photo, or signature. Evidence objects use short-lived presigned uploads and restricted access.

Driver/customer contact uses masked channels and is limited to the active delivery. Live location collection and viewing stop when the delivery reaches a terminal state.

## 12. Agent Execution and Governance

### 12.1 Execution Pattern

```text
message or delegated workflow
  -> authenticated principal and role context
  -> role graph and tool allowlist
  -> policy/ownership validation
  -> optional approval pause
  -> internal domain API command
  -> deterministic result
  -> redacted audit record
  -> user-visible response
```

Tool schemas reject unknown properties and validate identifiers, ranges, currency, and resource ownership. Domain APIs independently repeat authorization, approval, and state checks; trust is never based on text from the model.

### 12.2 Approval Contract

Consequential actions generate a canonical proposal containing tool, normalized arguments, affected resources, price/payout/refund impact, policy reason, and expiry. User confirmation creates `agent_approval` bound to the principal and proposal hash. Any argument change invalidates it. Approval is consumed atomically with the command unless policy permits reuse.

Approval is required for the role-specific cases in `SPEC.md`, including order placement, material substitutions, address/tip changes, bulk/large vendor changes, delivery acceptance unless enabled by policy, marking delivered, unassignment, and out-of-policy credits/refunds.

### 12.3 Memory and Safety

- Store only approved preference/memory categories with source and consent version.
- Do not place payment credentials, authentication secrets, private contact details, or unrestricted location history in model context.
- Separate system instructions, trusted tool results, and untrusted retrieved content.
- Enforce output schemas and tool allowlists; prompting is a defense-in-depth control only.
- Redact sensitive values before model calls and logs.
- Rate limit by user, workload, conversation, and tool.
- Provide global and role-level agent disable switches while preserving manual UI workflows.
- Route low-confidence, policy-blocked, repeated-failure, and high-risk cases to support.

## 13. Search and Retrieval

Exact SQL/PostGIS filters determine tenant, vendor open state, service eligibility, availability, price, category, allergen metadata, and distance. Semantic ranking may reorder only the eligible result set. Embeddings cover vendor-authored descriptions, policies, and product text; they never establish live commerce facts.

Search projections update from durable domain events. The system targets visibility within 10 seconds. Checkout always reads authoritative domain state, not the search projection or cache.

## 14. API Standards

### 14.1 Conventions

- JSON over HTTPS under `/v1`; internal tools under `/internal/v1` are network- and identity-restricted.
- OpenAPI is generated and contract-tested in CI.
- Mutation requests that can duplicate effects require `Idempotency-Key`.
- Versioned mutable resources use `If-Match` or a body `version`; stale writes return `409`.
- Collection APIs use opaque cursor pagination and bounded page sizes.
- Every response includes or echoes `X-Correlation-ID`.
- Times use RFC 3339 UTC and money uses `{amountMinor, currency}`.
- Errors use RFC 9457 problem details with stable application error codes and field violations.
- Clients may retry `429`, `502`, `503`, and `504` with bounded exponential backoff and jitter; mutations retain the same idempotency key.

### 14.2 Required External Resources

The endpoint set in `SPEC.md` is retained and expanded with:

```text
POST   /v1/carts/{cartId}/checkout
POST   /v1/orders/{orderId}/cancel
POST   /v1/vendor-orders/{id}/substitutions
POST   /v1/substitutions/{id}/decision
POST   /v1/deliveries/{id}/stops/{stopId}/arrival
POST   /v1/deliveries/{id}/stops/{stopId}/pickup
POST   /v1/deliveries/{id}/complete
POST   /v1/support-cases/{id}/evidence
GET    /v1/agent-actions
GET    /v1/agent-memory
PATCH  /v1/agent-memory/{id}
DELETE /v1/agent-memory/{id}
POST   /v1/webhooks/stripe
```

Status mutation routes accept commands rather than arbitrary target states. The server derives legal next states.

## 15. Events and Asynchronous Processing

Business transactions insert an `outbox_event` in the same transaction as aggregate changes. A publisher sends events to the durable transport and marks delivery progress. Consumers deduplicate by event ID and are safe under at-least-once delivery.

Every event includes schema version, unique event ID, aggregate ID/type/version, occurred time, actor, correlation ID, and causation ID. Personally identifying payload is minimized.

Required Phase 1 events include all events in `SPEC.md`, plus:

- `quote.created` and `quote.expired`
- `order.canceled`
- `vendor_order.preparation_updated`
- `delivery.offer_expired`
- `delivery.stop_collected`
- `payment.authorization_failed`
- `payment.capture_failed`
- `notification.requested`
- `support_case.escalated`
- `agent.action_completed`

After bounded retries, failed events move to a dead-letter queue with an operator-visible alert and replay tooling. Replays preserve event IDs or carry an explicit replay identifier.

## 16. Messaging, Support, and Vendor Publishing

- Transactional notifications are requested from committed domain events and delivered through in-app/push with policy-defined SMS or email fallback.
- Notification preferences do not suppress legally or operationally mandatory messages.
- Conversations expose masked contact details and enforce participant/order scope.
- Support cases collect issue type, narrative, structured item/delivery references, and evidence.
- Rules can resolve straightforward cases within configured limits; ambiguous, repeated, high-value, or fraud-flagged cases enter an operator queue.
- Vendors manually compose announcements/offers with products, discount, audience, channel, and schedule.
- Publication requires vendor approval, validation of ownership and timing, and an audit record.
- No Phase 1 process derives promotions from POS sales data.

## 17. Security and Privacy Controls

- TLS 1.2+ in transit and provider-managed encryption at rest.
- Secrets and signing keys in AWS Secrets Manager with rotation and no source-control copies.
- Private subnets and security groups restrict databases, caches, and internal APIs.
- Object storage blocks public access, uses scoped presigned URLs, malware scanning, content-type checks, and retention policies.
- Administrative and agent actions are append-only and queryable by authorized operators/users.
- Logs prohibit access tokens, payment instruments, raw secrets, and unnecessary message/evidence content.
- Rate limits and abuse controls exist at edge, account, IP risk, agent, and tool levels.
- Threat models cover account takeover, tenant escape, payment fraud, webhook spoofing, inventory races, CSV injection, prompt injection, evidence access, and location leakage.
- Dependency, container, secret, and static analysis run in CI; critical unresolved findings block production release.
- PCI scope is minimized through hosted/provider payment collection.
- Data classifications and approved retention periods are recorded before production launch.

## 18. Reliability, Performance, and Operations

### 18.1 Service Objectives

| Measure | Requirement |
| --- | --- |
| Standard API reads | p95 under 500 ms excluding providers |
| Standard API writes | p95 under 1 second excluding payment/routing providers |
| Agent first response | p95 under 3 seconds for non-tool turns |
| Inventory/search visibility | within 10 seconds |
| State notification emission | within 5 seconds after commit |
| Core monthly availability | 99.9% for checkout, order, inventory, delivery status |

Stricter internal objectives may be tracked but do not replace these acceptance requirements without an approved product change.

### 18.2 Resilience

- Provider calls use short timeouts, bounded retries where safe, circuit breakers, and idempotency.
- The domain service remains usable through manual workflows when the agent or model provider is unavailable.
- Redis failure may reduce caching/rate-limit precision but cannot corrupt source-of-truth state.
- Search lag cannot affect checkout correctness.
- Payment and event reconciliation jobs identify and repair ambiguous outcomes.
- Schema migrations are backward-compatible during rolling deployment and have tested rollback/forward-fix procedures.

### 18.3 Observability

OpenTelemetry propagates correlation across HTTP, jobs, provider calls, and events. Dashboards and alerts cover latency/error SLOs, checkout failures, reservation expiry, payment mismatch, webhook lag, outbox/DLQ depth, assignment conflicts, notification failures, agent tool denial, token/cost usage, and tenant-boundary violations.

Audit logs answer who proposed, approved, executed, and observed every consequential agent and administrative action.

### 18.4 Backup and Recovery

- PostgreSQL uses Multi-AZ deployment, automated backups, and point-in-time recovery.
- Object storage uses versioning where policy permits.
- Target recovery point is 15 minutes; target recovery time is 4 hours for the MVP, subject to business approval.
- Restore and regional dependency procedures are exercised before launch and at least twice yearly.

## 19. Deployment and Delivery

Local development uses Docker Compose for the domain API, agent service, PostgreSQL with pgvector/PostGIS, Redis, MinIO, and a local durable queue emulator.

AWS deployment uses an ALB/WAF edge, ECS Fargate services across availability zones, RDS PostgreSQL Multi-AZ, ElastiCache, S3, Secrets Manager, ECR, and EventBridge/SQS. Datastores and internal services remain private.

CI stages:

1. Formatting, linting, unit tests, and generated-contract checks.
2. Dependency, secret, static security, and container scans.
3. Integration tests with PostgreSQL/Redis/object/event dependencies.
4. Consumer/provider contract tests and database migration tests.
5. Build immutable images and generate an SBOM.
6. Deploy to staging and run end-to-end, accessibility, and smoke tests.
7. Require approval for production, use rolling or blue/green deployment, and run post-deploy checks.

## 20. Testing Strategy

- Unit tests cover pricing/rounding, policy decisions, state guards, route constraints, and authorization rules.
- Property and concurrency tests prove no oversell, no duplicate assignment, ledger balance, and idempotent command behavior.
- Integration tests cover database transactions, outbox publication, webhook replay, object upload, and provider adapters.
- Contract tests cover public clients, internal agent tools, events, and provider webhook schemas.
- End-to-end tests cover customer checkout, vendor acceptance/rejection, substitutions, multi-stop pickup, delivery, cancellation, and refund.
- Agent evaluations test unauthorized purchase, policy evasion, hallucinated availability, privacy leakage, prompt injection, malformed tools, and correct escalation.
- Accessibility tests combine automated checks with keyboard and screen-reader review against WCAG 2.2 AA.
- Load tests validate service objectives and race-heavy inventory/offer workflows at approved launch capacity.
- Resilience tests inject agent, payment, queue, cache, and notification failures and verify manual/degraded operation.

## 21. Acceptance Traceability

| Product capability | Technical control | Primary verification |
| --- | --- | --- |
| Agent-assisted order with confirmation | Scoped approval plus checkout saga | Agent evaluation and checkout E2E |
| Multi-vendor coordinated delivery | Parent/vendor orders plus delivery stops | Multi-vendor E2E |
| Configurable `X` and immutable snapshot | Fee configuration and quote/order snapshots | Pricing unit/property tests |
| Role-scoped agents | Workload delegation, tool allowlists, domain authorization | Authorization integration tests |
| Auditable consequential actions | Approval/action records and correlation | Audit integration tests |
| Safe form and CSV inventory | OCC forms and staged atomic import | Import integration/E2E tests |
| No final-unit oversell | Conditional reservation transaction | Parallel concurrency test |
| Retry-safe state changes | Idempotency store, guarded states, outbox dedupe | Replay and fault-injection tests |
| Timely customer updates | Durable events and notification workers | Event-latency SLO test |
| Manual workflows without AI | Direct domain APIs and role UIs | Agent-disabled E2E suite |
| Vendor manual offers | Approval-based publishing module | Vendor publishing E2E |
| Support and policy refunds | Cases, deterministic limits, escalation | Support/refund E2E |

## 22. Open Decisions and Configuration

The open product decisions in `SPEC.md` remain unresolved. Each must receive an owner, due date, decision record, and approved value before the affected production feature launches.

Runtime policy configuration includes effective date, scope, version, author, approver, and audit history. It covers platform fee `X`, quote/reservation lifetimes, route/vendor/freshness limits, partial rejection, capture/cancellation, auto-order/auto-accept, refund authority, delivery evidence, payout/settlement, and retention. Unsafe missing values fail closed.

Provider choices for maps, messaging, identity verification, model inference, and UCP/SPT availability are hidden behind tested adapters. A provider substitution must not alter domain state contracts.

## 23. Definition of Ready for Implementation

Engineering implementation may start when:

- Product and engineering approve this boundary and data-ownership model.
- Security approves the threat model and sensitive-data classifications.
- Payments confirms authorization, partial acceptance, capture, transfer, and refund flows.
- Operations approves SLO, alert, backup, and recovery ownership.
- Initial market rules and every checkout-blocking configuration have approved values.
- OpenAPI, event schemas, state transition tables, and database migrations are reviewed for the first delivery slice.

Production launch additionally requires all MVP acceptance criteria in `SPEC.md`, the verification matrix above, provider certification requirements, accessibility review, restore exercise, and agent safety evaluation to pass.
