# Agent-Native Marketplace Capability Specification

**Version:** 1.0
**Status:** Draft for implementation and audit
**Phase:** Phase 1 foundation with identified later-phase extensions
**Last updated:** 2026-09-13

## 1. Purpose

AgentCommerce provides the expected capabilities of a modern local-delivery marketplace through personal, role-scoped agents. The product is not only a conversational wrapper around conventional screens: agents discover information, monitor events, coordinate with other agents, recommend decisions, and execute approved actions through deterministic domain APIs.

"Similar to DoorDash" means comparable marketplace capability categories, not copying proprietary implementation, branding, UI, ranking, or every feature released by another service. Capability delivery remains phased and governed by `SPEC.md`.

## 2. Operating Model

```text
Customer or Vendor
        |
   voice / text / UI
        |
Role-scoped personal agent
        |
policy + consent + approval gate
        |
authenticated REST tools and domain events
        |
commerce services and auditable state
```

Agents may reason, summarize, rank, and draft. Only deterministic services may commit catalog, inventory, offer, order, payment, delivery, or refund state.

## 3. Agent-Native Capability Baseline

| Marketplace capability | Customer agent experience | Vendor agent experience |
| --- | --- | --- |
| Store discovery | Finds eligible nearby vendors by request, preference, cuisine, rating, timing, fee, or offer. | Maintains discoverable store facts, service options, hours, and service area. |
| Menu discovery | Shows and explains current menus, variants, modifiers, allergens, availability, and pricing. | Creates and updates menu content within approval policy and flags incomplete or conflicting data. |
| Recommendations | Suggests relevant items/vendors and explains price, availability, timing, dietary fit, and offer eligibility. | Suggests substitutes, bundles, upsells, and service options based on approved catalog facts and current operations. |
| Cart and checkout | Builds and updates carts, obtains quotes, and executes only after required approval. | Receives resulting sub-orders and applies vendor decision policies. |
| Live order management | Summarizes vendor decisions, substitutions, timing, and delivery changes. | Accepts/rejects within policy, adjusts preparation estimates, manages availability, and marks readiness. |
| Out-of-stock handling | Applies pre-approved fallback or asks the customer with clear price/allergen/time impact. | Marks item unavailable, proposes ranked alternatives, and sends a structured proposal to the customer agent. |
| Messaging and inquiries | Asks vendors questions without exposing unnecessary private data and tracks replies. | Answers approved facts automatically or routes exceptions to an authorized vendor user. |
| Offers and discounts | Finds eligible published offers and reviews vendor-specific proposals. | Drafts, targets, schedules, and sends approved offers with deterministic eligibility and limits. |
| Custom requests | Sends structured requests for accommodations, tiffin/meal service, recurring needs, or custom terms. | Accepts, declines, asks a follow-up, or returns an expiring approved proposal. |
| Feedback | Submits rating, structured feedback, private issue, or support case. | Receives and summarizes feedback, drafts responses, identifies trends, and escalates sensitive cases. |
| Notifications | Receives actionable summaries rather than raw operational noise. | Receives prioritized alerts for stock, orders, inquiries, approvals, feedback, and exceptions. |
| Manual fallback | Completes every core workflow through UI when the agent is unavailable. | Completes every core workflow through vendor portal when the agent is unavailable. |

Driver and operator experiences retain the dispatch, delivery, support, verification, safety, fraud, and configuration capabilities defined in `SPEC.md` and are exposed through their own scoped agents.

## 4. Vendor Agent Capabilities

### 4.1 Menu and Inventory

The vendor may ask:

- "Mark paneer tikka unavailable for tonight."
- "Increase lunch-box inventory to 30."
- "Which items are low stock?"
- "Import this menu and show me errors before changing anything."

The Vendor Agent must:

1. Read authoritative location-level catalog and inventory.
2. validate ownership, location membership, item state, input, and version.
3. Show the proposed mutation and operational impact.
4. Execute automatically only inside the vendor's approved inventory policy.
5. Request approval for bulk, price, destructive, cross-location, or high-value changes.
6. Commit through catalog/inventory APIs with idempotency and optimistic concurrency.
7. Emit inventory events so discovery and customer agents update within the visibility objective.
8. Record proposal, approval basis, tool call, result, and correlation ID.

### 4.2 Out-of-Stock and Substitution Coordination

```text
Inventory/order event
    -> Vendor Agent identifies unavailable item
    -> Vendor Agent ranks valid alternatives
    -> structured substitution proposal
    -> Customer Agent checks preferences and policy
       -> exact pre-approved match: accept autonomously
       -> otherwise: ask customer
    -> Order Service commits accepted substitution
    -> pricing/payment/inventory are reconciled
    -> both parties receive the outcome
```

A proposed replacement includes item identity, quantity, price delta, allergen/dietary data, availability, preparation-time delta, expiry, and vendor rationale. Neither agent may claim a substitution is accepted until the Order Service confirms it.

### 4.3 Recommendations

Vendor recommendations may include substitutes, complementary products, bundles, popular items, service plans, or better-fit options. They must:

- Use available, orderable catalog items and approved vendor facts.
- Respect customer request, dietary constraints, budget, consent, and communication policy.
- Clearly identify sponsored, discounted, or vendor-funded recommendations.
- Explain material price and timing differences.
- Avoid manipulative ranking, fabricated scarcity, hidden fees, or unsupported health/allergen claims.
- Never add an item to a cart without customer instruction or a valid pre-approved rule.

### 4.4 Offers

The Vendor Agent may draft:

- Item or category discounts.
- Spend-threshold discounts.
- Buy-one-get-one or free-item offers.
- Delivery-fee incentives where platform policy permits.
- Bundles and meal combinations.
- New, existing, repeat, or lapsed-customer offers.
- Customer-specific custom proposals.
- Tiffin or recurring meal-service proposals.

Every offer defines funding source, eligible vendor locations/items/audience, discount calculation, minimums, maximum benefit, redemption limit, stacking rule, budget, start/end time, cancellation terms, and approval status. The agent may publish only under a valid vendor approval or a narrowly scoped pre-approved campaign policy.

### 4.5 Customer Inquiries and Custom Requests

An inquiry is a structured conversation linked to a customer, vendor location, optional cart/order, topic, status, expiry, consent context, and correlation ID.

Supported topics include:

- Product or ingredient questions.
- Dietary or preparation accommodations.
- Item availability and expected return time.
- Tiffin, catering, scheduled, or recurring meal service.
- Repeat-customer or volume discount requests.
- Delivery or pickup constraints.
- Other vendor-defined service questions.

The Vendor Agent may answer from published facts or vendor-approved response policies. New commercial terms, allergen-sensitive commitments, refunds, credits, recurring obligations, or operational exceptions require vendor approval. A negotiation does not itself create an order or payment obligation.

### 4.6 Feedback

The Vendor Agent may:

- Receive ratings, structured tags, written feedback, and private support feedback.
- Summarize trends by location, item, shift, and issue category.
- Draft a factual, respectful response for vendor review or policy-approved sending.
- Propose catalog, preparation, packaging, or service improvements.
- Create follow-up tasks or support cases.

The agent must not disclose customer private data, retaliate, offer prohibited incentives for positive reviews, suppress legitimate negative feedback, or independently resolve safety, fraud, discrimination, chargeback, or high-value refund cases.

## 5. Autonomy Levels

| Level | Behavior | Examples |
| --- | --- | --- |
| A0 Observe | Read, monitor, summarize, recommend; no mutation. | Low-stock summary, feedback trends, menu explanation. |
| A1 Pre-approved | Execute a narrowly defined action inside saved policy and limits. | Mark zero-stock item unavailable; send approved factual answer; accept exact substitution rule. |
| A2 Confirm each action | Prepare the action and require owner/customer approval before execution. | New offer, price change, custom tiffin proposal, order placement. |
| A3 Human/operator required | Stop and route to authorized staff. | Allergy ambiguity, safety issue, suspected fraud, policy exception, large refund. |

Every autonomy policy is actor-owned, versioned, revocable, purpose-specific, bounded by amount/time/scope, and evaluated server-side at execution time. Silence is not approval unless an explicit timeout policy defines a safe non-financial fallback.

## 6. Required Domain Contracts

### 6.1 REST Tool Families

- `/v1/vendor-locations/{locationId}/catalog`
- `/v1/vendor-locations/{locationId}/inventory`
- `/v1/vendor-locations/{locationId}/orders`
- `/v1/vendor-locations/{locationId}/offers`
- `/v1/vendor-locations/{locationId}/inquiries`
- `/v1/vendor-locations/{locationId}/feedback`
- `/v1/customers/me/preferences`
- `/v1/customers/me/agent-inbox`
- `/v1/customers/me/vendor-inquiries`
- `/internal/v1/agent-tools/...` for equivalent scoped agent operations

Final endpoint shapes are governed by generated OpenAPI contracts. Agent routes must call the same application services and policy checks as manual routes.

### 6.2 Event Families

- `catalog.item.changed`
- `inventory.availability.changed`
- `inventory.low_stock.detected`
- `order.vendor_action_required`
- `order.substitution.proposed`
- `order.substitution.resolved`
- `vendor.inquiry.created`
- `vendor.inquiry.responded`
- `vendor.offer.proposed`
- `vendor.offer.approved`
- `vendor.offer.published`
- `customer.feedback.created`
- `customer.feedback.response_proposed`

Events use the envelope, outbox, deduplication, correlation, causation, replay, and authorization requirements in `TECH_SPEC.md`.

## 7. Approval Rules

Vendor confirmation is required by default for:

- Product price changes and bulk/destructive catalog changes.
- New campaign publication, budget, audience, or funding commitment.
- Customer-specific discounts or terms outside an existing policy.
- Tiffin, recurring, catering, or volume commitments not represented by an approved service definition.
- Allergen-sensitive or materially different substitutions.
- Refunds, credits, or compensation outside configured limits.
- Public responses to sensitive feedback.

Customer confirmation is required by default for order placement, non-pre-approved substitutions, recurring order/payment mandates, material price or timing changes, and acceptance of custom commercial terms.

## 8. Marketplace Coverage and Phasing

Phase 1 targets agent-native coverage for discovery, menus, inventory, ordering, live fulfillment, substitutions, messaging, offers, custom inquiries, feedback, delivery, payment, and support. Manual portal equivalents remain mandatory.

Later releases may add platform memberships, full loyalty points, advertising auctions, POS-driven optimization, advanced scheduled/recurring billing, group orders, pickup, catering orchestration, and other marketplace extensions through explicit phase decisions. Their absence does not permit the system to advertise unsupported functionality.

## 9. External Capability References

The scope categories were cross-checked against official DoorDash materials available on 2026-09-13:

- Merchant order fulfillment, availability, customer/Dasher contact, and substitutions: <https://help.doordash.com/en-us/merchants/article/fulfill-orders>
- Merchant promotions and offer types: <https://help.doordash.com/en-us/advertising/article/overview-of-promotions>
- Customer substitution preferences: <https://help.doordash.com/en-nz/consumers/article/what-are-customer-substitution-preferences>
- Store-member loyalty deals: <https://help.doordash.com/en-us/consumers/article/store-member-loyalty-deals>

These references inform capability categories only. AgentCommerce requirements, policies, and acceptance criteria remain defined by this repository.

## 10. Acceptance Scenarios

1. Vendor says, "Mark samosas out of stock and suggest pakoras." The agent previews or applies the permitted inventory change and sends a structured replacement proposal to affected customer agents.
2. Customer says, "Show this restaurant's menu and today's discounts." The agent returns only current, eligible, authoritative items and offers.
3. Customer asks whether daily tiffin service is available. The vendor agent answers from an approved service definition or requests vendor review and returns a tracked response.
4. Vendor asks the agent to offer repeat customers 10% off this weekend. The agent drafts eligibility, funding, limits, budget, and dates; publication waits for approval.
5. Customer submits packaging feedback. The vendor agent records it, summarizes the issue, drafts a response, and escalates if the message indicates safety or refund risk.
6. Agent attempts an unapproved price change or marketing message. Server-side policy rejects it and records the denial.
7. Agent services are unavailable. Vendor and customer complete the same core workflows through REST-backed portal screens.