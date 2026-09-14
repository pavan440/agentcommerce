# Functionality Catalog and Audit Baseline

**Version:** 1.0
**Status:** Requirements extracted; implementation not yet assessed
**Last updated:** 2026-09-13

## 1. Purpose

This document converts the approved product, technical, architecture, UI, and data specifications into a traceable functionality checklist. It is the baseline for the next implementation audit. A row marked `Not assessed` makes no claim about whether the repository currently implements that requirement.

## 2. Source Authority

1. `SPEC.md` defines product behavior and release scope.
2. `TECH_SPEC.md` defines implementation, security, API, event, reliability, and testing requirements.
3. `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` is authoritative for cloud deployment. GCP is approved; AWS is not a target platform.
4. Domain architecture, UI, and database documents refine their respective areas but cannot silently broaden Phase 1 scope.
5. `SPEC_VALIDATION.md` records unresolved product decisions and production-readiness gaps.

## 3. Audit Method

| Status | Meaning |
| --- | --- |
| Not assessed | Evidence has not yet been inspected. |
| Implemented | Code, tests, API behavior, and documentation satisfy the acceptance statement. |
| Partial | Some acceptance conditions are present, but material behavior or verification is missing. |
| Not implemented | No sufficient implementation evidence exists. |
| Blocked | Assessment depends on an unresolved product decision or unavailable external system. |
| Not applicable | Requirement is intentionally outside the audited release, with rationale recorded. |

Each future audit result must include evidence such as source paths, test names, API responses, migration objects, or deployment configuration. Presence of a table or endpoint alone is not proof of complete functionality.

## 4. Release Scope

- **Phase 1:** Marketplace foundation, role portals, role agents, catalog and inventory, multi-vendor ordering, payment, dispatch, tracking, support, and manually authored announcements/offers.
- **Phase 2:** POS integration, sales intelligence, automated promotion recommendations, redemption attribution, and advanced real-time voice experiences.
- Phase 2 extension points may exist in Phase 1, but Phase 2 services are not Phase 1 dependencies.

## 5. Functionality Requirements

### 5.1 Identity, Access, and User Data

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| IAM-001 | 1 | All | OIDC access tokens are validated for signature, issuer, audience, expiry, and required scopes. | `TECH_SPEC.md` §4 | Not assessed |
| IAM-002 | 1 | All | Authorization combines role, resource ownership, vendor/location membership, account status, workflow state, and consent where applicable. | `TECH_SPEC.md` §4 | Not assessed |
| IAM-003 | 1 | Vendor/Driver/Operator | Sensitive actions require stronger verification according to policy. | `SPEC.md` §12; `TECH_SPEC.md` §4 | Not assessed |
| IAM-004 | 1 | Customer | Customer can view and update profile, addresses, preferences, dietary needs, and substitution policy. | `SPEC.md` §4.1 | Not assessed |
| IAM-005 | 1 | User | Location tracking and agent personalization use separate versioned consent records; revocation stops future collection. | `TECH_SPEC.md` §4 | Not assessed |
| IAM-006 | 1 | User | User can view, correct, export, and delete eligible account and agent-memory data, with retention exceptions disclosed. | `SPEC.md` §5.5; `TECH_SPEC.md` §4 | Not assessed |
| IAM-007 | 1 | Platform | Credentials, access tokens, and payment instruments are absent from profile tables and logs. | `TECH_SPEC.md` §4, §17 | Not assessed |

### 5.2 Vendor and Location Management

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| VEN-001 | 1 | Vendor | Authorized vendor user can create and maintain a vendor business profile. | `SPEC.md` §4.2, §9.2 | Not assessed |
| VEN-002 | 1 | Vendor | Vendor can manage locations, operating hours, service areas, tax, fees, and preparation-time settings. | `SPEC.md` §4.2 | Not assessed |
| VEN-003 | 1 | Operator | Operator can review, activate, suspend, and audit vendor status subject to policy. | `SPEC.md` §4.4, §12 | Not assessed |
| VEN-004 | 1 | Platform | Every vendor/location read and mutation is tenant-scoped and rejects unauthorized membership. | `TECH_SPEC.md` §4, §6 | Not assessed |
| VEN-005 | 1 | Vendor | Vendor can review order history and operational metrics for its own locations only. | `SPEC.md` §4.2 | Not assessed |
| VEN-006 | 1 | Vendor | Vendor can configure versioned agent autonomy policies for inventory, order decisions, substitutions, communications, and offers. | `SPEC.md` §4.2, §5.3 | Not assessed |
| VEN-007 | 1 | Vendor | Vendor can use its agent to query and update menu and inventory through the same validated, tenant-scoped services as the portal. | `SPEC.md` §4.2, §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.1 | Not assessed |
| VEN-008 | 1 | Vendor | Vendor receives customer inquiries, custom requests, proposals, and feedback in a prioritized role-scoped inbox. | `SPEC.md` §4.2, §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.5-4.6 | Not assessed |
| VEN-009 | 1 | Vendor | Vendor can approve, decline, modify, or expire agent-drafted substitutions, custom terms, and offers. | `SPEC.md` §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4, §7 | Not assessed |
| VEN-010 | 1 | Vendor | All core vendor-agent capabilities retain equivalent manual portal workflows when the agent is unavailable. | `TECH_SPEC.md` §18; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §3 | Not assessed |

### 5.3 Catalog and Discovery

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| CAT-001 | 1 | Vendor | Vendor can manage products, variants, modifiers, prices, categories, descriptions, allergen metadata, and active status. | `SPEC.md` §4.2, §11 | Not assessed |
| CAT-002 | 1 | Customer | Customer can browse and search nearby eligible vendors and products using exact and semantic retrieval. | `SPEC.md` §4.1, §11 | Not assessed |
| CAT-003 | 1 | Customer | Discovery defaults to vendors that are open, serviceable, and currently available. | `SPEC.md` §9.1, §11 | Not assessed |
| CAT-004 | 1 | Customer | Results expose price, options, allergens, fees, availability, and estimated timing needed for a purchase decision. | `SPEC.md` §11 | Not assessed |
| CAT-005 | 1 | Platform | Search-index lag cannot determine checkout correctness; checkout revalidates authoritative state. | `TECH_SPEC.md` §18 | Not assessed |

### 5.4 Inventory and CSV Import

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| INV-001 | 1 | Vendor | Inventory is maintained per location and SKU with quantities, status, reorder threshold, source, timestamp, and version data. | `SPEC.md` §10 | Not assessed |
| INV-002 | 1 | Vendor | System supports quantity-tracked and availability-only prepared-food inventory. | `SPEC.md` §10 | Not assessed |
| INV-003 | 1 | Vendor | Portal supports search/filter, inline edit, bulk activation/deactivation, and quantity adjustment. | `SPEC.md` §10 | Not assessed |
| INV-004 | 1 | Vendor | UTF-8 CSV import supports the specified required and optional fields. | `SPEC.md` §10 | Not assessed |
| INV-005 | 1 | Vendor | CSV rows are staged and validated with row errors/warnings, a summary, exclusion of invalid rows, and explicit commit. | `SPEC.md` §10; `TECH_SPEC.md` §8 | Not assessed |
| INV-006 | 1 | Vendor | Committed imports have an audit ID and roll back only when affected records have not subsequently changed. | `SPEC.md` §10; `TECH_SPEC.md` §8 | Not assessed |
| INV-007 | 1 | Platform | Inventory updates use optimistic concurrency and reject stale writes. | `SPEC.md` §10; `TECH_SPEC.md` §8 | Not assessed |
| INV-008 | 1 | Platform | Reservations are atomic, short-lived, idempotent, auto-expire, commit on acceptance, and release on rejection/cancellation without overselling. | `SPEC.md` §10; `TECH_SPEC.md` §8 | Not assessed |
| INV-009 | 1 | Customer | Inventory and search visibility update within 10 seconds of an authoritative change. | `SPEC.md` §10; `TECH_SPEC.md` §18 | Not assessed |

### 5.5 Cart, Quote, and Checkout

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| CQT-001 | 1 | Customer | Customer can add quantities, variants, and modifiers from multiple eligible vendors to one vendor-grouped cart. | `SPEC.md` §9.1, §11 | Not assessed |
| CQT-002 | 1 | Platform | Cart enforces configured vendor count, radius, route, freshness, minimum, and availability constraints. | `SPEC.md` §11 | Not assessed |
| CQT-003 | 1 | Customer | Quote shows subtotals, taxes, discounts, tips, delivery charges, and transparent marketplace X fee before approval. | `SPEC.md` §9.1, §11 | Not assessed |
| CQT-004 | 1 | Platform | X fee uses configured basis points and half-up rounding; missing required pricing configuration fails closed. | `TECH_SPEC.md` §7 | Not assessed |
| CQT-005 | 1 | Platform | Quote snapshots material inputs, expires, and is invalidated by material cart or configuration changes. | `TECH_SPEC.md` §7 | Not assessed |
| CQT-006 | 1 | Customer | Checkout requires explicit confirmation of quote, delivery details, substitution policy, payment, and consequential agent action. | `SPEC.md` §5, §9.1 | Not assessed |
| CQT-007 | 1 | Platform | Retried checkout commands cannot create duplicate orders or charges. | `TECH_SPEC.md` §9, §14 | Not assessed |

### 5.6 Orders and Substitutions

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| ORD-001 | 1 | Platform | Checkout creates one parent order and one sub-order per participating vendor. | `SPEC.md` §9.1 | Not assessed |
| ORD-002 | 1 | Vendor | Vendor receives only its sub-order and can accept, reject, set preparation timing, prepare, and mark ready. | `SPEC.md` §9.2 | Not assessed |
| ORD-003 | 1 | Platform | Order module exclusively applies state transitions and records actor, source, time, reason, versions, and correlation. | `SPEC.md` §12; `TECH_SPEC.md` §9 | Not assessed |
| ORD-004 | 1 | Platform | Duplicate or invalid transitions produce no repeated financial, inventory, or notification effects. | `SPEC.md` §12 | Not assessed |
| ORD-005 | 1 | Customer | Configured partial-rejection policy applies when vendors reject a multi-vendor order. | `SPEC.md` §9.1 | Not assessed |
| ORD-006 | 1 | Customer/Vendor | Substitution policy controls proposals, approval, timeout, removal, and refund behavior. | `SPEC.md` §9.4 | Not assessed |
| ORD-007 | 1 | Platform | Substitution price changes adjust authorization/capture safely and are auditable. | `SPEC.md` §9.4; `TECH_SPEC.md` §10 | Not assessed |
| ORD-008 | 1 | Customer | Customer can view status and cancel when policy permits with correct release/refund effects. | `SPEC.md` §4.1, §9.1 | Not assessed |

### 5.7 Payments and Settlement

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| PAY-001 | 1 | Customer | Payment method is collected through provider-hosted mechanisms that minimize PCI scope. | `SPEC.md` §13; `TECH_SPEC.md` §10, §17 | Not assessed |
| PAY-002 | 1 | Platform | Authorization precedes submission and capture/adjustment follows approved acceptance and substitution policy. | `SPEC.md` §9.1; `TECH_SPEC.md` §10 | Not assessed |
| PAY-003 | 1 | Platform | Provider abstraction supports Stripe PaymentIntents/Connect, SPT/UCP when available, and PaymentMethod fallback. | `SPEC.md` §13; `TECH_SPEC.md` §10 | Not assessed |
| PAY-004 | 1 | Platform | Signed webhooks are verified, deduplicated, reconciled asynchronously, and traceable to immutable ledger records. | `TECH_SPEC.md` §10 | Not assessed |
| PAY-005 | 1 | Vendor | Transfers and marketplace amounts reconcile to accepted order components and refunds. | `SPEC.md` §13; `TECH_SPEC.md` §10 | Not assessed |
| PAY-006 | 1 | Operator | Refund workflows are deterministic, idempotent, policy-limited, and audited. | `SPEC.md` §16; `TECH_SPEC.md` §10, §16 | Not assessed |

### 5.8 Driver Dispatch and Delivery

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| DSP-001 | 1 | Driver | Driver can complete onboarding, submit verification data, and maintain service zones and availability. | `SPEC.md` §4.3 | Not assessed |
| DSP-002 | 1 | Operator | Operator can record driver verification and activate or suspend eligibility. | `SPEC.md` §4.4, §12 | Not assessed |
| DSP-003 | 1 | Driver | Eligible driver receives an expiring offer showing route, stops, time/distance, handling details, and payout. | `SPEC.md` §9.3, §14 | Not assessed |
| DSP-004 | 1 | Driver | Driver can accept/decline; assignment is atomic, unique, idempotent, and reassignable after expiry/failure. | `SPEC.md` §9.3; `TECH_SPEC.md` §11 | Not assessed |
| DSP-005 | 1 | Driver/Vendor | Each vendor pickup is separately navigated and verified before handoff. | `SPEC.md` §9.2, §9.3 | Not assessed |
| DSP-006 | 1 | Driver/Customer | Delivery supports navigation, masked contact, arrival, proof, issue reporting, and completion. | `SPEC.md` §9.3; `TECH_SPEC.md` §11 | Not assessed |
| DSP-007 | 1 | Platform | Location collection is consented, scoped to active work, access-controlled, and stops at terminal delivery state. | `TECH_SPEC.md` §4, §11, §17 | Not assessed |
| DSP-008 | 1 | Driver | Payout reflects the complete accepted route and is not silently reduced after acceptance. | `SPEC.md` §9.3, §14 | Not assessed |

### 5.9 Messaging, Support, and Publishing

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| MSG-001 | 1 | Customer/Vendor/Driver | Order-scoped messaging uses in-app or masked contact and enforces participant authorization. | `SPEC.md` §15; `TECH_SPEC.md` §16 | Not assessed |
| MSG-002 | 1 | Platform | Committed state changes emit in-app/push notifications within five seconds with policy-defined fallback. | `SPEC.md` §15; `TECH_SPEC.md` §18 | Not assessed |
| MSG-003 | 1 | User | Preferences are honored except for legally or operationally mandatory messages. | `TECH_SPEC.md` §16 | Not assessed |
| MSG-004 | 1 | Customer/Vendor | Role-scoped agent inboxes persist authorized order events, inquiries, proposals, offers, custom requests, and feedback with correlation and lifecycle status. | `SPEC.md` §9.5; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §3, §6 | Not assessed |
| MSG-005 | 1 | Vendor | Proactive vendor-agent communication enforces consent, audience, frequency caps, opt-out, privacy, and abuse policy. | `SPEC.md` §9.5; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.3-4.4 | Not assessed |
| SUP-001 | 1 | All roles | User can open an issue with type, narrative, structured references, and evidence. | `SPEC.md` §16; `TECH_SPEC.md` §16 | Not assessed |
| SUP-002 | 1 | Operator | Ambiguous, repeated, high-value, safety, or fraud-flagged cases enter an authorized queue. | `SPEC.md` §4.4, §16 | Not assessed |
| PUB-001 | 1 | Vendor | Vendor can manually author an announcement/offer with products, discount, audience, channel, and schedule. | `SPEC.md` §17 | Not assessed |
| PUB-002 | 1 | Vendor | Publication validates ownership/timing, requires approval, and creates an audit record. | `SPEC.md` §17; `TECH_SPEC.md` §16 | Not assessed |
| PUB-003 | 1 | Vendor | Vendor agent can draft item, threshold, BOGO/free-item, delivery incentive, bundle, repeat-customer, and custom offers. | `SPEC.md` §9.8; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.4 | Not assessed |
| PUB-004 | 1 | Platform | Offer funding, eligibility, stacking, limits, budget, validity, cancellation, and redemption are deterministic and visible. | `SPEC.md` §9.8; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.4 | Not assessed |

### 5.10 Agents, Memory, Approval, and Handoff

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| AGT-001 | 1 | All | Each role agent uses authenticated domain APIs and never directly accesses transactional stores or providers. | `SPEC.md` §5; `TECH_SPEC.md` §2, §13 | Not assessed |
| AGT-002 | 1 | Customer | Customer agent supports discovery, vendor/availability inspection, cart, quote, approved order, status, cancellation, and support. | `SPEC.md` §5.1 | Not assessed |
| AGT-003 | 1 | Vendor | Vendor agent supports incoming orders, decisions, preparation, substitutions, status, and scoped inventory query/import. | `SPEC.md` §5.2 | Not assessed |
| AGT-004 | 1 | Driver | Driver agent supports availability, offers, assignment, routing, arrival, pickup, contact, delivery, and issues. | `SPEC.md` §5.3 | Not assessed |
| AGT-005 | 1 | All | Agent explains recommendations and consequences, distinguishes facts from estimates, and avoids uncertain guarantees. | `SPEC.md` §5 | Not assessed |
| AGT-006 | 1 | All | Consequential actions require canonical, expiring approvals bound to principal, action hash, scope, and financial impact. | `SPEC.md` §5; `TECH_SPEC.md` §13 | Not assessed |
| AGT-007 | 1 | All | Agent treats content as untrusted and enforces tool schemas, least privilege, policy checks, and audit. | `SPEC.md` §5; `TECH_SPEC.md` §2, §13 | Not assessed |
| AGT-008 | 1 | All | Agent hands off for authorization, ambiguity, safety, allergy, price tolerance, delay, or no-match conditions. | `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` | Not assessed |
| AGT-009 | 1 | Customer | Autonomous rerouting occurs only for exact pre-approved preferences within tolerance and remains visible. | `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` | Not assessed |
| AGT-010 | 1 | User | Agent memory is consented, purpose-limited, inspectable, correctable, deletable where eligible, and role-protected. | `SPEC.md` §5.5; `TECH_SPEC.md` §13 | Not assessed |
| AGT-011 | 1 | Platform | Manual deterministic workflows remain available when agent/model services fail. | `SPEC.md` §5; `TECH_SPEC.md` §18 | Not assessed |
| AGT-012 | 1 | Customer | Customer agent evaluates current vendor menus, authoritative availability, fees, timing, dietary constraints, and saved preferences before recommending items. | `SPEC.md` §5.2 | Not assessed |
| AGT-013 | 1 | Customer | Customer can inspect, create, update, and revoke preferences and bounded autonomy rules through the agent. | `SPEC.md` §5.2 | Not assessed |
| AGT-014 | 1 | Customer | Customer agent consumes authorized vendor/order events, records them in an agent inbox, notifies the customer, and summarizes actionable changes. | `SPEC.md` §5.2 | Not assessed |
| AGT-015 | 1 | Customer/Vendor | Role agents exchange structured proposals through authenticated APIs/events and execute only decisions allowed by current approval or pre-approved policy. | `SPEC.md` §5.1-5.3; `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` | Not assessed |
| AGT-016 | 1 | Platform | Free-form agent-to-agent communication cannot directly commit order, inventory, payment, or delivery state. | `SPEC.md` §5.1; `TECH_SPEC.md` §2, §13 | Not assessed |
| AGT-017 | 1 | Customer | Customer can ask by voice or text to show a vendor menu, compare vendors, or find currently eligible published discounts. | `SPEC.md` §5.2 | Not assessed |
| AGT-018 | 1 | Customer/Vendor | Customer agent can create and track a structured inquiry about unpublished services or terms such as tiffin service or repeat-customer discounts. | `SPEC.md` §5.2-5.3; `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` §6 | Not assessed |
| AGT-019 | 1 | Vendor | Vendor agent answers from approved facts/policies or obtains vendor-owner approval before proposing custom prices, recurring plans, discounts, or exceptions. | `SPEC.md` §5.3; `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` §6 | Not assessed |
| AGT-020 | 1 | Customer | Customer agent presents an expiring vendor proposal with price, cadence, conditions, cancellation terms, and validity before requesting acceptance. | `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` §6 | Not assessed |
| AGT-021 | 1 | Platform | Inquiry or negotiation alone never creates an order, recurring charge, or subscription; resulting commerce requires explicit authorization. | `docs/architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md` §6 | Not assessed |
| AGT-022 | 1 | Vendor | Vendor agent monitors authoritative inventory, recommends corrections, and performs item availability or quantity updates only within approved limits. | `SPEC.md` §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.1 | Not assessed |
| AGT-023 | 1 | Customer/Vendor | An out-of-stock proposal includes replacement, quantity, availability, price delta, allergens, timing delta, rationale, and expiry. | `SPEC.md` §5.2-5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.2 | Not assessed |
| AGT-024 | 1 | Vendor | Vendor agent may recommend available substitutes, complements, bundles, or service plans while respecting request relevance, dietary constraints, budget, consent, and promotion disclosure. | `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.3 | Not assessed |
| AGT-025 | 1 | Vendor | Vendor agent can draft, target, schedule, and send offers only under explicit approval or a narrowly scoped pre-approved campaign policy. | `SPEC.md` §5.3, §9.8; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.4 | Not assessed |
| AGT-026 | 1 | Customer/Vendor | Vendor agent answers custom requests from published facts/policies and escalates new commercial, recurring, allergen-sensitive, or operational commitments. | `SPEC.md` §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.5 | Not assessed |
| AGT-027 | 1 | Vendor | Vendor agent records and summarizes feedback, drafts respectful responses, proposes improvements, and escalates safety, fraud, abuse, or refund-sensitive cases. | `SPEC.md` §5.3, §9.5; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4.6 | Not assessed |
| AGT-028 | 1 | Platform | Agent actions are classified as observe, pre-approved, confirm-each-action, or human/operator-required and enforced server-side. | `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §5 | Not assessed |
| AGT-029 | 1 | Vendor | Agent cannot fabricate availability, discounts, scarcity, allergen claims, custom terms, or vendor commitments. | `SPEC.md` §5.1, §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4 | Not assessed |
| AGT-030 | 1 | Customer/Vendor | Every inquiry, proposal, approval, denial, expiration, message, and resulting action is correlated and auditable. | `TECH_SPEC.md` §13, §15; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §4-6 | Not assessed |

### 5.11 Portals and Accessibility

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| UI-001 | 1 | Customer | Responsive customer UI covers discovery, configuration, cart, approval, tracking, and support. | `SPEC.md` §3-4; `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` | Not assessed |
| UI-002 | 1 | Vendor | Tablet-friendly vendor UI covers settings, catalog/inventory, CSV, orders, preparation, metrics, and publishing. | `SPEC.md` §3-4; `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` | Not assessed |
| UI-003 | 1 | Driver | Mobile driver UI covers onboarding, availability, offers, navigation, pickup, proof, contact, issues, and payout. | `SPEC.md` §3-4; `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` | Not assessed |
| UI-004 | 1 | Operator | Console supports safety, support, disputes/refunds, fraud, verification, configuration, and operations. | `SPEC.md` §4.4 | Not assessed |
| UI-005 | 1 | All | Phase 1 provides a text conversational dock and action cards without making conversation the sole workflow. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` | Not assessed |
| UI-006 | 1 | All | Interfaces meet WCAG 2.2 AA including keyboard, focus, labels, contrast, errors, and reduced motion. | `SPEC.md` §19; `TECH_SPEC.md` §20 | Not assessed |
| UI-007 | 1 | Vendor | Vendor portal and voice/text agent expose inventory, order, inquiry, offer, custom-request, feedback, approval, and agent-policy controls. | `SPEC.md` §4.2, §5.3; `docs/architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md` §3-5 | Not assessed |
| UI-008 | 1 | Customer | A distinct customer application supports Android, iOS, tablet, and responsive desktop web with discovery, ordering, tracking, support, and a persistent personal-agent entry point. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2, §6 | Not assessed |
| UI-009 | 1 | Vendor | A distinct vendor application supports Android, iOS, tablet, and responsive desktop web with operational dashboards and agent-assisted workflows. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2, §5 | Not assessed |
| UI-010 | 1 | Driver | A distinct dasher application supports Android and iOS as primary targets, with a responsive desktop operational fallback. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2, §8 | Not assessed |
| UI-011 | 1 | Operator | A distinct admin application is desktop-first, large-tablet responsive, keyboard complete, and protected by operator authorization. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2, §4 | Not assessed |
| UI-012 | 1 | All | Four role apps share design tokens and API primitives but maintain separate navigation, authorization boundaries, notification channels, and role-scoped agent tools. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2 | Not assessed |
| UI-013 | 1 | All | Phase 1 supports text and push-to-talk input with visual confirmation; real-time interruptible bidirectional voice remains Phase 2. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2, §7 | Not assessed |
| UI-014 | 1 | All | Installable PWAs provide immediate browser coverage; App Store/Play Store packaging requires separate native build, signing, and release verification. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` §2; `docs/ui/MARKETPLACE_PORTAL.md` | Not assessed |

### 5.12 API, Events, Security, and Operations

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| PLT-001 | 1 | API client | Public JSON APIs use HTTPS `/v1`; internal agent tools use protected `/internal/v1`. | `TECH_SPEC.md` §14 | Not assessed |
| PLT-002 | 1 | API client | OpenAPI is generated/validated and contract-tested; errors use RFC 9457 with stable codes and field details. | `TECH_SPEC.md` §14 | Not assessed |
| PLT-003 | 1 | API client | Commands support idempotency keys, versioned updates support concurrency, and lists use cursor pagination. | `TECH_SPEC.md` §14 | Not assessed |
| PLT-004 | 1 | Platform | Effects propagate `X-Correlation-ID`; timestamps use RFC 3339 UTC and money uses minor units plus currency. | `TECH_SPEC.md` §5, §14 | Not assessed |
| PLT-005 | 1 | Platform | Critical effects use transactional outbox and at-least-once Cloud Pub/Sub delivery with idempotent consumers. | `TECH_SPEC.md` §2, §15, §19 | Not assessed |
| PLT-006 | 1 | Platform | Events carry versions, time, actor, correlation, and causation and support DLQ, alerting, and replay. | `TECH_SPEC.md` §15 | Not assessed |
| PLT-007 | 1 | Platform | Modules own their data; cross-module direct table writes are prohibited. | `TECH_SPEC.md` §2, §6 | Not assessed |
| PLT-008 | 1 | Platform | TLS, encryption, Secret Manager, private services, protected objects, rate limits, audit, and log controls are enforced. | `TECH_SPEC.md` §17 | Not assessed |
| PLT-009 | 1 | Platform | OpenTelemetry covers HTTP, jobs, providers, and events with required dashboards and alerts. | `TECH_SPEC.md` §18 | Not assessed |
| PLT-010 | 1 | Platform | Core availability is 99.9%; reads meet p95 500 ms, writes p95 1 s, and agent response p95 3 s under exclusions. | `TECH_SPEC.md` §18 | Not assessed |
| PLT-011 | 1 | Platform | Backup/recovery targets RPO 15 minutes and RTO 4 hours with regular restore exercises. | `TECH_SPEC.md` §18 | Not assessed |
| PLT-012 | 1 | Platform | CI covers unit, integration, contract, migration, E2E, accessibility, resilience, security, SBOM, and deploy checks. | `TECH_SPEC.md` §19-20 | Not assessed |

### 5.13 GCP Deployment

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| GCP-001 | 1 | Platform | Domain API and agent service deploy as containerized Cloud Run services. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §2-3 | Not assessed |
| GCP-002 | 1 | Platform | Cloud Load Balancing and Cloud Armor provide edge routing, WAF, and DDoS controls. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §3 | Not assessed |
| GCP-003 | 1 | Platform | Cloud SQL PostgreSQL 16 provides PostGIS and pgvector with backups and approved availability. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §3 | Not assessed |
| GCP-004 | 1 | Platform | Memorystore, Pub/Sub, GCS, Secret Manager, and Artifact Registry provide cache, events, objects, secrets, and images. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §3, §5 | Not assessed |
| GCP-005 | 1 | Platform | Requests route to the correct community cell with tenant-safe data partitioning. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §4 | Not assessed |
| GCP-006 | 1 | Platform | Cloud Build and Terraform/gcloud deploy immutable artifacts through controlled environments to Cloud Run cells. | `docs/architecture/GCP_CELL_BASED_COMMUNITY_DEPLOYMENT_ARCHITECTURE.md` §5 | Not assessed |

### 5.14 Phase 2 Capabilities

| ID | Phase | Actor | Acceptance statement | Source | Status |
| --- | --- | --- | --- | --- | --- |
| P2-001 | 2 | Vendor | POS connections use canonical adapters, secure credentials, health state, and approved connectivity. | `docs/architecture/AI_POS_INTEGRATION_AGENT_AND_PROMOTIONS_ENGINE.md` | Not assessed |
| P2-002 | 2 | Platform | POS catalog, inventory, order, and sales ingestion is idempotent, observable, replayable, and canonically mapped. | `docs/architecture/AI_POS_INTEGRATION_AGENT_AND_PROMOTIONS_ENGINE.md` | Not assessed |
| P2-003 | 2 | Vendor | Sales intelligence detects slow periods and recommends explained strategies with vendor approval. | `SPEC.md` §18; `docs/architecture/AI_POS_INTEGRATION_AGENT_AND_PROMOTIONS_ENGINE.md` | Not assessed |
| P2-004 | 2 | Vendor/Operator | Promotion redemption, attribution, limits, and ROI are measurable across marketplace and in-store channels. | `docs/architecture/AI_POS_INTEGRATION_AGENT_AND_PROMOTIONS_ENGINE.md` | Not assessed |
| P2-005 | 2 | All | Real-time voice preserves visual confirmation, accessibility, approval, interruption, and fallback behavior. | `docs/ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md` | Not assessed |

## 6. Canonical Order States

### Parent Order

`draft` → `quoted` → `inventory_reserved` → `payment_authorized` → `submitted` → `partially_accepted` or `accepted` → `driver_assigned` → `pickups_in_progress` → `out_for_delivery` → `delivered`

Exceptional terminal/financial states: `canceled`, `partially_refunded`, `refunded`.

### Vendor Sub-order

`pending_vendor` → `accepted` → `preparing` → `ready_for_pickup` → `picked_up` → `delivered`

Exceptional states: `rejected`, `canceled`, `refunded`.

Only the order domain may apply transitions. Audit must verify state guards, terminal-state behavior, duplicate safety, and side-effect idempotency.

## 7. Minimum End-to-End Acceptance Scenarios

1. Customer creates a multi-vendor cart, approves a transparent quote, and creates one parent order with reserved inventory and authorized payment.
2. Vendors independently accept/reject, prepare, substitute within policy, and mark sub-orders ready; parent and payment outcomes follow partial-acceptance policy.
3. One eligible driver accepts a complete offer atomically, verifies each pickup, completes combined delivery with proof, and receives agreed payout.
4. Concurrent buyers cannot oversell inventory, and duplicate checkout/webhook/event delivery creates no duplicate effect.
5. Customer, vendor, driver, and operator cannot access another tenant's or actor's protected resources.
6. Agent presents material effects, receives scoped approval, executes once, and leaves a complete audit trail.
7. Agent/model outage still permits core manual workflows.
8. Vendor stages a mixed-validity CSV, excludes invalid rows, commits valid rows, and rolls back only while versions are unchanged.
9. Cancellation, rejection, substitution, and refund reconcile inventory, payments, transfers, ledger, and notifications.
10. GCP staging routes a request to the correct community cell and verifies private data access, observability, backup, and recovery controls.

## 8. Unresolved Product Decisions

These decisions may cause audit rows to be marked `Blocked` where exact behavior cannot be determined:

- Launch jurisdiction, supported commerce categories, and regulated-item policy.
- Marketplace X fee, delivery fee, minimum order, driver payout, radius, route, vendor count, and freshness limits.
- Partial vendor rejection and payment capture/cancellation timing.
- Whether auto-ordering or vendor auto-accept is allowed in the MVP.
- Refund thresholds, fraud rules, ranking policy, delivery proof, and retention periods.
- Final providers for identity verification, maps, messaging, push, and model inference.

GCP is not unresolved. It is the approved cloud platform and replaces prior AWS references.

## 9. Audit Deliverable Format

For every requirement, the implementation audit adds:

| Field | Required evidence |
| --- | --- |
| Status | One status from §3. |
| Implementation | Source file, class/module, migration, configuration, or UI path. |
| Verification | Unit/integration/contract/E2E test or reproducible API/UI check. |
| Gap | Missing behavior, edge case, security control, test, or documentation. |
| Recommended action | Smallest concrete change required to reach `Implemented`. |

Audit Phase 1 actor workflows first, then authorization and state integrity, API/events, GCP deployment, and Phase 2 extension points.