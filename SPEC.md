# Agentic Community Delivery Marketplace Specification

## 1. Document Purpose

This document defines an agentic, community-based local delivery marketplace. The platform presents local vendors to their community and connects customers, vendors, and drivers. Each participant has a dedicated AI agent that can perform approved tasks, communicate with other platform agents, and escalate decisions to a human when confirmation is required.

The first release focuses on restaurants, convenience stores, and other neighborhood merchants. A customer can combine products from multiple vendors into one coordinated delivery.

## 2. Product Vision

Users should be able to complete most delivery tasks through natural-language conversations:

- A customer agent discovers products, builds carts, places orders, tracks deliveries, and resolves common issues.
- A vendor agent manages incoming orders, inventory, availability, substitutions, and preparation status.
- A driver agent evaluates offers, plans multiple pickups and one customer drop-off, reports progress, and handles delivery exceptions.
- The platform coordinates all agents using deterministic order, payment, dispatch, and inventory services.
- The marketplace helps community members discover and support nearby vendors.

AI agents assist with decisions but do not bypass platform rules. Money movement, order commitments, refunds above configured limits, and sensitive account changes require deterministic validation and, where specified, explicit human approval.

## 3. Goals

### 3.1 MVP Goals

- Support account creation and role-specific onboarding for customers, vendors, and drivers.
- Provide a conversational AI agent for each role.
- Allow customers to discover local vendors and products, create a multi-vendor cart, check out, track one coordinated delivery, and request support.
- Allow vendors to accept, reject, prepare, and complete pickup handoff for orders.
- Allow drivers to accept delivery offers containing multiple vendor pickups and update delivery status.
- Allow vendors to maintain inventory through web forms and CSV uploads.
- Keep product availability synchronized with ordering in near real time.
- Maintain an auditable record of agent suggestions, tool calls, approvals, and outcomes.
- Apply a configurable `X%` platform fee to eligible products based on their vendor-declared in-store prices.

### 3.2 Non-Goals for MVP

- Fully autonomous purchasing without customer-configured limits and confirmation rules.
- Autonomous driver vehicle operation.
- Payroll, tax filing, insurance underwriting, or driver background-check processing.
- Combining deliveries for multiple customers in one driver route.
- Advertising auctions, subscriptions, and loyalty programs.
- International currencies, taxes, and regulatory models.

### 3.3 Business Model

- Vendors provide the in-store base price for each product.
- The platform applies a configurable fee rate named `X` to eligible product prices.
- `X` is not fixed for MVP planning because pricing has not yet been market-tested.
- The implementation must store `X` as configuration and must not hardcode a percentage in application code.
- The product platform fee is calculated as `in_store_price * (X / 100)`.
- The customer product price before tax is calculated as `in_store_price + product_platform_fee`.
- Vendor product settlement is based on the in-store base price, and the platform retains the product platform fee.
- The quote must clearly show the base product total, platform fee, delivery fee, taxes, tip, discounts, and final total.
- Every order stores the value of `X` and all calculated price components as immutable snapshots.
- Changes to `X` affect new quotes only and never alter an accepted quote or submitted order.

## 4. Users and Roles

### 4.1 Customer

A person ordering products for delivery.

Key capabilities:

- Set delivery addresses, preferences, dietary needs, and substitution rules.
- Search by vendor, item, cuisine, price, rating, availability, or delivery estimate.
- Ask the customer agent for recommendations or repeat purchases.
- Review and approve carts, fees, tips, and payment totals.
- Track order and driver progress.
- Report missing, incorrect, damaged, or late items.

### 4.2 Vendor

A merchant that sells products and prepares orders.

Key capabilities:

- Configure store details, operating hours, service area, taxes, fees, and preparation times.
- Manage products, variants, modifiers, prices, and inventory.
- Update inventory using forms or CSV files.
- Accept or reject orders and propose substitutions.
- Update preparation and pickup readiness.
- Review order history and operational metrics.

### 4.3 Driver

A delivery worker who transports an order from one or more vendors to a customer.

Key capabilities:

- Set availability and delivery zone.
- Receive delivery offers with estimated distance, time, all pickup details, and payout.
- Accept or decline offers.
- Navigate to pickup and delivery locations.
- Confirm pickup and delivery with required evidence.
- Report delays, access problems, unsafe conditions, or failed delivery attempts.

### 4.4 Platform Operator

An internal administrator who manages safety, support, disputes, fraud, configuration, and marketplace operations.

## 5. Agent Model

Each role-specific agent is an AI interface backed by a restricted set of platform tools. Agents may reason and recommend actions, but business services remain the source of truth.

### 5.1 Shared Agent Requirements

Every agent must:

- Operate only on behalf of an authenticated user or platform workflow.
- Use role-scoped tools and least-privilege authorization.
- Explain material actions before requesting approval.
- Show price, fees, payout, or refund impact before a financial commitment.
- Request confirmation when required by policy.
- Never expose another party's private contact, payment, or account data.
- Treat chat text, product descriptions, CSV contents, and external content as untrusted input.
- Record prompts, decisions, tool calls, approvals, errors, and results in an audit log.
- Hand off to a person or support queue when confidence is low or policy blocks an action.
- Avoid guaranteeing inventory, timing, refunds, or outcomes until confirmed by the relevant service.

### 5.2 Customer Agent

Responsibilities:

- Translate natural-language requests into catalog searches and filters.
- Recommend vendors and items using explicit customer constraints.
- Build and modify carts.
- Identify unavailable items and suggest alternatives.
- Present subtotal, taxes, fees, tip, estimated arrival, and final total.
- Place an order only after required customer confirmation.
- Monitor order events and summarize status.
- Initiate policy-allowed cancellation, refund, or support workflows.

Customer agent tools:

- `search_catalog`
- `get_vendor_details`
- `get_item_availability`
- `create_cart`
- `update_cart`
- `quote_order`
- `place_order`
- `get_order_status`
- `cancel_order`
- `create_support_case`

Required approval examples:

- Placing an order unless the customer has enabled a valid auto-order policy.
- Accepting a substitution that increases the total beyond the configured tolerance.
- Changing the delivery address after checkout.
- Adding or changing a tip after delivery.

### 5.3 Vendor Agent

Responsibilities:

- Summarize new orders and identify operational risks.
- Accept or reject orders according to vendor-configured rules.
- Estimate preparation time.
- Detect unavailable products and propose substitutions.
- Update order preparation status.
- Answer inventory questions and surface low-stock products.
- Assist with form updates and CSV import correction.

Vendor agent tools:

- `get_incoming_orders`
- `accept_order`
- `reject_order`
- `set_preparation_time`
- `propose_substitution`
- `update_order_status`
- `query_inventory`
- `update_inventory`
- `validate_inventory_import`
- `commit_inventory_import`

Required approval examples:

- Bulk inventory changes above a configurable item or value threshold.
- Price changes above a configured percentage.
- Rejecting an already accepted order.
- Issuing vendor-funded credits outside predefined policy.

### 5.4 Driver Agent

Responsibilities:

- Summarize delivery offers, including payout, estimated time, distance, and known constraints.
- Recommend whether an offer matches driver preferences without auto-accepting by default.
- Provide pickup and drop-off instructions.
- Monitor trip progress and suggest status updates.
- Communicate sanitized delay or arrival messages.
- Guide exception workflows such as closed stores, unavailable parking, or unreachable customers.

Driver agent tools:

- `set_driver_availability`
- `get_delivery_offers`
- `accept_delivery_offer`
- `decline_delivery_offer`
- `get_route`
- `confirm_arrival_at_vendor`
- `confirm_pickup`
- `contact_party`
- `confirm_delivery`
- `report_delivery_issue`

Required approval examples:

- Accepting an offer unless the driver enables policy-compliant auto-accept rules.
- Marking an order delivered.
- Canceling or unassigning an accepted delivery.
- Sharing a live location outside an active delivery.

### 5.5 Agent Memory

- The customer agent remembers customer-approved preferences, dietary needs, favorite products and vendors, delivery instructions, substitution choices, and previous orders.
- The vendor agent remembers vendor-approved catalog details, operating preferences, preparation patterns, inventory patterns, substitution rules, and order-handling policies.
- Customers and vendors can view, correct, or delete their agent memory.

## 6. Core User Flows

### 6.1 Customer Ordering Flow

1. Customer asks for products, vendors, meals, or community recommendations.
2. Customer agent searches only active local vendors and currently orderable inventory.
3. Customer agent presents options with base prices, the `X%` platform fee, delivery costs, and estimated delivery time.
4. Customer selects items from one or more vendors or asks the agent to build a cart.
5. Cart service groups items into a vendor sub-order for each vendor location.
6. Cart service validates prices, modifiers, inventory, address, vendor hours, pickup route feasibility, and coordinated delivery limits.
7. Quote service calculates product platform fees, taxes, delivery fee, tip, discounts, and final total.
8. Customer reviews and confirms the final multi-vendor quote.
9. Order service creates one pending parent order, creates vendor sub-orders, and reserves inventory per vendor.
10. Payment service authorizes payment for the complete order.
11. Order service submits the authorized parent order to the vendors.
12. Each vendor receives and accepts or rejects only its own sub-order.
13. The platform applies the configured partial-rejection policy if one vendor rejects its sub-order.
14. Dispatch service offers one coordinated multi-pickup delivery to eligible drivers.
15. Customer receives event-driven updates for each vendor and the overall delivery.
16. Payment is captured or adjusted according to vendor acceptance and platform policy.
17. The parent order completes after all accepted sub-orders are delivered together.

### 6.2 Vendor Fulfillment Flow

1. Vendor receives a new sub-order alert containing only products from that vendor.
2. Vendor agent summarizes items, substitutions, notes, and requested delivery time.
3. Vendor accepts with a preparation estimate or rejects with a reason.
4. Reserved inventory becomes committed when the vendor accepts.
5. Vendor prepares the order and marks it ready for pickup.
6. Driver provides the pickup verification code or platform identity.
7. Vendor confirms handoff.
8. The vendor sub-order changes to `picked_up` after driver confirmation.

### 6.3 Driver Delivery Flow

1. Dispatch confirms that accepted vendor sub-orders can be collected within configured distance, timing, and freshness limits.
2. Dispatch identifies eligible online drivers.
3. Driver receives an offer showing all pickup stops, pickup order, total distance, estimated time, constraints, and payout.
4. Driver accepts or declines within the offer window.
5. Driver follows the planned route and confirms arrival at each vendor.
6. Driver verifies and confirms each pickup separately.
7. Driver travels to the customer after collecting all accepted sub-orders.
8. Driver follows delivery instructions and captures required proof.
9. Driver confirms the combined delivery.
10. Customer is notified and the delivery payout is finalized.

### 6.4 Substitution Flow

1. The customer selects a substitution mode in their profile or for a specific order.
2. In preference-based mode, the customer agent may choose a replacement only when it matches the customer's saved preferences.
3. In approval-required mode, the customer agent recommends a replacement but must receive customer approval before accepting it.
4. Vendor reports an ordered item unavailable.
5. Inventory is immediately marked unavailable or reduced.
6. Vendor agent proposes one or more in-stock alternatives.
7. Customer agent applies the customer's selected substitution mode.
8. Payment authorization and order totals are adjusted after a substitution is accepted.
9. If no substitution is accepted before the timeout, the item is removed and refunded according to policy.

## 7. Inventory Management

### 7.1 Inventory Concepts

Inventory is maintained per vendor location and sellable item or variant.

Each inventory record includes:

- Vendor location
- SKU
- Product or variant reference
- Quantity on hand
- Quantity reserved
- Quantity available
- Availability status
- Reorder threshold
- Price override, if allowed
- Last update source
- Last updated timestamp
- Version number for concurrency control

`quantity_available = quantity_on_hand - quantity_reserved`

Vendors may choose either:

- Quantity tracking for countable goods.
- Availability-only tracking for prepared food or items without reliable counts.

### 7.2 Form-Based Updates

The vendor portal must provide:

- Search and filtering by product, SKU, category, and status.
- Inline quantity and availability editing.
- Bulk selection for activate, deactivate, or quantity adjustment.
- Product creation and editing forms.
- Validation before save.
- Optimistic concurrency protection to prevent overwriting newer changes.
- Audit history showing who or what changed each value.

### 7.3 CSV Import

The system must support `.csv` files encoded as UTF-8. A downloadable template must be available.

Required columns:

```csv
sku,name,price,quantity_on_hand,is_available
```

Optional columns:

```csv
description,category,variant_name,reorder_threshold,currency,operation
```

Rules:

- `sku` must be unique within a vendor location.
- `price` must be a non-negative decimal with at most two fractional digits for USD.
- `price` represents the vendor's in-store base price before the configurable `X%` platform fee.
- `quantity_on_hand` must be a non-negative integer when quantity tracking is enabled.
- `is_available` must accept `true` or `false`.
- `operation` may be `upsert`, `update`, or `deactivate`; the default is `upsert`.
- Unknown columns generate warnings but do not fail the import.
- Duplicate SKUs, malformed rows, invalid values, and missing required fields generate row-level errors.
- The import must not partially apply until the vendor reviews a validation summary and confirms it.
- The confirmation screen must show creates, updates, deactivations, warnings, and errors.
- Imports containing errors cannot be committed unless invalid rows are explicitly excluded.
- Every committed import receives an ID and audit record.
- A committed import can be rolled back when none of its affected records have since changed.

Example:

```csv
sku,name,price,quantity_on_hand,is_available,category,operation
BURGER-001,Classic Burger,12.99,50,true,Burgers,upsert
FRIES-001,Regular Fries,4.49,80,true,Sides,upsert
SHAKE-VAN,Vanilla Shake,5.99,0,false,Drinks,update
```

### 7.4 Inventory Consistency

- Checkout uses a short-lived inventory reservation.
- Reservations expire automatically if payment or order creation fails.
- Vendor acceptance commits reserved quantities.
- Vendor rejection or customer cancellation releases reservations.
- Inventory updates use version checks to prevent lost updates.
- An item with insufficient available quantity cannot be added to a new cart.
- Active carts are revalidated before checkout; cart presence does not guarantee stock.
- Inventory changes publish events so search and catalog views update within 10 seconds.
- Negative available inventory is prohibited unless a vendor-specific oversell policy is enabled in a future release.

## 8. Order State Model

Parent order states:

```text
draft
quoted
inventory_reserved
payment_authorized
submitted
partially_accepted
accepted
driver_assigned
pickups_in_progress
out_for_delivery
delivered
canceled
partially_refunded
refunded
```

Vendor sub-order states:

```text
pending_vendor
accepted
rejected
preparing
ready_for_pickup
picked_up
delivered
canceled
refunded
```

Rules:

- State transitions are enforced by the order service, not by an AI agent.
- Every transition records actor, source, timestamp, and reason.
- A parent order becomes `accepted` when all vendor sub-orders are accepted and `partially_accepted` when only some are accepted.
- Parent terminal states are `delivered`, `canceled`, and `refunded`.
- Vendor sub-order terminal states are `rejected`, `delivered`, `canceled`, and `refunded`.
- Refunds may be full or partial and have a separate payment lifecycle.
- Invalid or duplicate transition requests return the current state safely and do not repeat side effects.

## 9. Functional Requirements

### 9.1 Identity and Access

- Support email or phone authentication.
- Support role-based access control and vendor-location membership.
- Require stronger verification for vendor administrators and drivers.
- Store payment details through a PCI-compliant payment provider; the platform must not store raw card numbers.
- Require explicit consent for location tracking and agent personalization.

### 9.2 Catalog and Search

- Search active vendors and products by text and structured filters.
- Exclude closed vendors and unavailable products by default.
- Display item prices, modifiers, allergens supplied by the vendor, and availability.
- Display estimated fees and delivery time before checkout.
- Support semantic search while preserving exact filters and policy constraints.

### 9.3 Cart and Checkout

- Allow one cart to contain items from multiple eligible vendor locations.
- Group cart items by vendor and create one vendor sub-order per location.
- Enforce configurable maximum vendor count, pickup radius, route duration, and product freshness constraints.
- Show vendor-level subtotals and the combined delivery total.
- Validate required modifiers and quantity limits.
- Recalculate totals on every material cart change.
- Use idempotency keys for checkout and payment requests.
- Preserve the customer-confirmed quote for a short configured window.

### 9.4 Dispatch

- Match orders to online, eligible drivers by pickup route, distance, service zone, capacity, timing, and configured safety rules.
- Optimize pickup order while honoring vendor readiness and product freshness constraints.
- Show every pickup, total route, constraints, and payout before offer acceptance.
- Expire unanswered offers.
- Prevent assignment of one delivery to multiple drivers.
- Support reassignment after timeout, decline, or approved unassignment.

### 9.5 Messaging and Notifications

- Support in-app messaging with masked contact details.
- Allow agents to draft and send transactional messages within policy.
- Notify users about material order state changes.
- Support push notifications, with SMS or email fallback for critical events.
- Retain message records according to privacy and support policies.

### 9.6 Support and Disputes

- Let each role create a support case linked to an order or delivery.
- Collect structured issue type, description, and evidence.
- Apply deterministic refund rules for straightforward cases.
- Escalate ambiguous, high-value, repeated, or fraud-flagged claims to an operator.

### 9.7 Agent Commerce and Payment

- Support the Agentic Commerce Protocol (ACP) for agent access to catalog, inventory, cart, and checkout operations.
- Use Stripe Shared Payment Tokens (SPTs) for agent payments. Each SPT is issued after customer confirmation and limited to the platform's Stripe seller account, maximum amount, currency, and expiration.
- Complete the payment by confirming a Stripe PaymentIntent with the SPT.
- Use Stripe Connect separate charges and transfers to split the parent-order payment among multiple vendor accounts.
- Until SPT access is available, use Stripe PaymentMethods with PaymentIntents through the same payment-service interface.

### 9.8 Manual Vendor Announcements and Offers

- In Phase 1, vendors manually create announcements and offers.
- The vendor chooses the strategy, products, discount, audience, channel, start time, and end time.
- The platform publishes the vendor-approved announcement or offer to the community.
- Phase 1 does not include POS integrations or automated sales-based recommendations.

### 9.9 POS Sales Intelligence

- Integrate with each vendor's existing point-of-sale system to import in-store product, inventory, and sales data.
- Combine in-store and online sales data for analysis.
- Detect slow sales periods by hour, day, and season.
- Let the vendor define a strategy such as increasing in-store customer count, increasing online sales, or moving selected inventory.
- The vendor agent recommends an offer, products, channel, start time, and end time based on the selected strategy and observed sales pattern.
- The vendor approves an offer before it is announced in-store, online, or through community notifications.
- Track sales during the offer and compare results with the normal baseline.
- Example: when winter sales are repeatedly slow from 9:00 PM to the 11:00 PM closing time, the vendor agent can recommend announcing an offer at 8:00 PM for that evening.

## 10. Suggested Architecture

### 10.1 Client Applications

- Customer mobile app and responsive web app
- Driver mobile app
- Vendor web portal with tablet-friendly order view
- Operator administration console

### 10.2 Backend Services

- API gateway
- Identity and authorization service
- Agent orchestration service
- Customer profile service
- Vendor and location service
- Catalog and search service
- Inventory service
- Cart and quote service
- Order service
- Payment service
- Dispatch and delivery service
- Messaging and notification service
- Announcement and offer service
- Support and refund service
- Audit and analytics service
- POS integration and sales intelligence service for Phase 2

### 10.3 Storage and Infrastructure

- Relational database for transactional data.
- Search index for vendor and product discovery.
- Object storage for CSV imports, receipts, and delivery evidence.
- Cache for sessions, quotes, availability, and rate limiting.
- Event bus for order, inventory, dispatch, and notification events.
- Geospatial service or database extension for distance and service-zone queries.
- Secret manager for credentials and signing keys.

### 10.4 Agent Execution Pattern

```text
User message
  -> authenticated role context
  -> agent orchestrator
  -> policy and permission check
  -> role-scoped tool call
  -> deterministic domain service
  -> validated result
  -> audit event
  -> agent response
```

The agent orchestration service must not write directly to transactional databases. All mutations pass through authenticated domain APIs.

### 10.5 Hybrid Retrieval

- Agents query structured APIs for live menus, prices, inventory, availability, and operating hours.
- Agents use retrieval-augmented generation (RAG) for unstructured vendor information, descriptions, policies, and reviews.
- Structured API data is authoritative when RAG content conflicts with live commerce data.

## 11. Core Data Entities

- `User`: identity, contact methods, roles, status.
- `CustomerProfile`: preferences, saved addresses, substitution rules, agent settings.
- `DriverProfile`: verification status, vehicle, zones, availability, agent settings.
- `Vendor`: merchant identity and configuration.
- `VendorLocation`: address, hours, service settings, preparation settings.
- `Product`: vendor-owned sellable product.
- `ProductVariant`: SKU, price, modifiers, status.
- `InventoryRecord`: quantity, reservation, availability, version.
- `InventoryImport`: file metadata, validation status, summary, commit status.
- `Cart`: customer, vendor locations, grouped items, quote state.
- `Order`: parent transaction, customer, combined totals, address snapshot, status, timestamps.
- `VendorOrder`: vendor-specific child order, subtotal, status, preparation estimate, pickup state.
- `OrderItem`: purchased item snapshot, in-store price, applied `X`, platform fee, final unit price, quantity, modifiers, substitution state.
- `FeeConfiguration`: effective date, market, configurable `X`, status, and change history.
- `Payment`: authorization, capture, refund, provider references.
- `Delivery`: driver, route summary, pickup/drop-off status, payout.
- `DeliveryStop`: vendor pickup sequence, readiness, arrival, and collection status.
- `DeliveryOffer`: driver offer, payout, expiry, response.
- `Conversation`: participants, role context, retention policy.
- `AgentMemory`: owner, role, approved memory content, source, and timestamps.
- `AgentAction`: agent, user, tool, inputs hash, approval, result, timestamp.
- `Review`: customer, vendor, order reference, rating, and text.
- `Announcement`: vendor-authored community message, audience, channel, schedule, and status.
- `Offer`: vendor-defined products, discount, audience, channel, schedule, and status.
- `POSConnection`: Phase 2 vendor POS provider, location mapping, status, and synchronization state.
- `SalesRecord`: Phase 2 in-store or online sale, vendor, products, amount, channel, and timestamp.
- `PromotionRecommendation`: Phase 2 vendor strategy, observed pattern, recommended offer, and approval status.
- `SupportCase`: issue, evidence, resolution, financial outcome.

## 12. API Surface

Illustrative REST endpoints:

```text
GET    /v1/vendors
GET    /v1/vendors/{vendorId}/catalog
GET    /v1/products/{productId}/availability
POST   /v1/carts
PATCH  /v1/carts/{cartId}
POST   /v1/carts/{cartId}/quote
POST   /v1/orders
GET    /v1/orders/{orderId}
POST   /v1/orders/{orderId}/status
POST   /v1/orders/{orderId}/substitutions
POST   /v1/vendor-orders/{vendorOrderId}/accept
POST   /v1/vendor-orders/{vendorOrderId}/reject
POST   /v1/vendor-orders/{vendorOrderId}/status
POST   /v1/vendor-locations/{locationId}/announcements
POST   /v1/vendor-locations/{locationId}/offers
GET    /v1/vendor-locations/{locationId}/inventory
PATCH  /v1/vendor-locations/{locationId}/inventory/{sku}
POST   /v1/vendor-locations/{locationId}/inventory-imports
GET    /v1/inventory-imports/{importId}
POST   /v1/inventory-imports/{importId}/commit
POST   /v1/inventory-imports/{importId}/rollback
GET    /v1/drivers/me/offers
POST   /v1/delivery-offers/{offerId}/accept
POST   /v1/deliveries/{deliveryId}/status
POST   /v1/support-cases
POST   /v1/agent/conversations/{conversationId}/messages
```

Mutation endpoints must support idempotency keys where retries could create duplicate financial or order effects.

## 13. Domain Events

Key events:

- `inventory.updated`
- `inventory.import_validated`
- `inventory.import_committed`
- `inventory.reserved`
- `inventory.released`
- `order.submitted`
- `vendor_order.accepted`
- `vendor_order.rejected`
- `vendor_order.ready_for_pickup`
- `delivery.offer_created`
- `delivery.driver_assigned`
- `delivery.picked_up`
- `delivery.completed`
- `payment.authorized`
- `payment.captured`
- `refund.completed`
- `support_case.created`

Events must include a unique event ID, entity ID, entity version, timestamp, actor, and correlation ID.

## 14. Agent Safety and Governance

- Define an allowlist of tools for each agent role.
- Validate tool arguments against schemas and authenticated ownership.
- Require server-side policy checks even if the agent claims approval exists.
- Store approval records with the action they authorize.
- Redact secrets and unnecessary personal information from model context and logs.
- Detect prompt-injection attempts in vendor text, customer messages, CSV fields, and uploaded content.
- Do not let uploaded content alter agent permissions or system instructions.
- Apply per-user, per-agent, and per-tool rate limits.
- Support an emergency agent-disable switch without interrupting manual platform workflows.
- Provide a user-visible history of consequential agent actions.
- Evaluate agents for unauthorized purchases, incorrect status changes, privacy leakage, hallucinated availability, and policy evasion before release.

## 15. Non-Functional Requirements

### 15.1 Performance

- Standard API reads: p95 under 500 ms, excluding third-party services.
- Standard API writes: p95 under 1 second, excluding payment and routing providers.
- Agent first response: p95 under 3 seconds for non-tool conversational turns.
- Inventory changes visible to ordering and search within 10 seconds.
- Order state notifications emitted within 5 seconds of a committed transition.

### 15.2 Reliability

- Target 99.9% monthly availability for checkout, order, inventory, and delivery status services.
- Use idempotent writes and retry-safe event consumers.
- Prevent duplicate charges, orders, inventory commits, and driver assignments.
- Degrade to manual role workflows when the agent service is unavailable.

### 15.3 Security and Privacy

- Encrypt data in transit and at rest.
- Maintain role and tenant isolation at API and data-access layers.
- Log administrative and agent actions.
- Minimize location retention and restrict live location access to active deliveries.
- Support account data export and deletion according to applicable policy.
- Complete threat modeling for payments, account takeover, CSV uploads, agent tools, and location data.

### 15.4 Accessibility

- Target WCAG 2.2 AA for web experiences.
- Support screen readers, keyboard navigation, scalable text, sufficient contrast, and non-color status cues.
- Ensure critical workflows are available without voice input.

## 16. Delivery Phases

### Phase 1: Marketplace Foundation

- Identity, roles, customer/vendor/driver agents, vendor onboarding, catalog, inventory forms and CSV imports, customer browsing, multi-vendor cart, Stripe checkout, order management, dispatch, delivery tracking, and manual vendor-created announcements and offers.
- Vendors manually choose and approve their offer strategies in Phase 1.
- Phase 1 has no POS integrations or automated sales-based recommendations.

### Phase 2: POS Sales Intelligence

- Existing POS integrations, in-store sales tracking, combined online and in-store analytics, slow-period detection, vendor strategy selection, offer recommendations, vendor approval, and offer performance tracking.

## 17. MVP Acceptance Criteria

The MVP is ready when all of the following are true:

- A customer can ask their agent to find available items, create a cart, review a final quote, confirm payment, and place an order.
- A customer can combine products from multiple eligible community vendors into one order and receive them in one coordinated delivery.
- The quote applies the configured `X%` platform fee without hardcoding a percentage and shows the fee before customer confirmation.
- A submitted order retains its original `X` value even after platform configuration changes.
- The customer agent cannot place an order without valid confirmation or an explicitly configured auto-order policy.
- A vendor can accept its sub-order, set preparation time, report an unavailable item, propose a substitution, and mark the sub-order ready.
- A vendor can manually create and publish a community announcement or offer using its chosen strategy.
- A driver can review and accept an offer, confirm pickup, report an issue, and confirm delivery.
- Each agent can perform only its documented role-scoped actions.
- Every consequential agent action has an audit record and, when required, an approval record.
- A vendor can create and edit inventory through a form.
- A vendor can upload a CSV, review row-level errors and a change summary, and confirm the import.
- Invalid imports do not alter live inventory.
- Inventory reservations prevent two confirmed orders from consuming the same final unit.
- Inventory, order, payment, and delivery state changes are idempotent and recoverable after retry.
- The customer sees material order updates without manually refreshing.
- Manual customer, vendor, and driver workflows remain usable if AI agents are disabled.

## 18. Open Product Decisions

These decisions should be resolved before implementation planning:

- Initial launch market and applicable payment, tax, labor, privacy, and delivery regulations.
- Restaurant, convenience-store, and neighborhood retail categories eligible at launch.
- Market-test and select the initial value of `X`.
- Decide whether `X` varies by market, vendor category, or vendor agreement after testing.
- Delivery fee, minimum order, and driver payout formulas.
- Maximum vendors, pickup radius, route duration, and freshness rules for one delivery.
- Customer choice when one vendor rejects a sub-order: continue partially, substitute the vendor, or cancel the entire order.
- Payment capture and cancellation timing.
- Customer auto-order limits and whether auto-ordering ships in MVP.
- Driver auto-accept policy and whether it ships in MVP.
- Refund thresholds that agents may execute automatically.
- Delivery radius and dispatch ranking rules.
- Proof-of-delivery requirements by order value and delivery type.
- Data retention periods for conversations, location history, evidence, and agent audit logs.
- Preferred maps, messaging, identity verification, and model providers.
