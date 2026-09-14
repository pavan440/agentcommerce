# Technical Specification Validation Report

**Project:** Agentic Community Delivery Marketplace  
**Validated source:** `SPEC.md`  
**Candidate technical draft:** attached `pasted-text.txt`  
**Validation date:** 2026-09-11  
**Result:** Conditionally valid; revision required before implementation

## 1. Executive Summary

The candidate draft selects a credible implementation direction: a Spring Boot modular monolith for deterministic commerce, an isolated Python agent orchestrator, PostgreSQL, Redis, object storage, and a GCP community-cell deployment on Cloud Run. It also correctly keeps the AI layer away from direct transactional database access.

The draft is not yet suitable for its stated status, "Approved for Implementation." It covers architecture choices but does not fully specify the required behavior in `SPEC.md`. In particular, it lacks implementable contracts for order state, inventory reservation, multi-vendor payment adjustment, dispatch, agent approvals, support, notifications, privacy, audit retention, failure recovery, accessibility, and acceptance-test traceability.

The revised `TECH_SPEC.md` closes the implementation-level gaps while leaving unresolved product decisions explicitly configurable or marked as blockers.

## 2. Validation Method

The draft was checked against every section of `SPEC.md` using these criteria:

- Coverage: each Phase 1 requirement has a technical owner and implementation approach.
- Consistency: technical behavior does not contradict product rules or state models.
- Determinism: financial, inventory, order, and delivery mutations are enforced outside the LLM.
- Recoverability: retries, duplicate delivery, partial failure, and compensating actions are defined.
- Verifiability: requirements map to testable acceptance criteria and operational targets.
- Security: tenant isolation, least privilege, approval evidence, privacy, and untrusted content are addressed.

## 3. Findings

| ID | Severity | Finding | Required correction |
| --- | --- | --- | --- |
| V-01 | Blocker | The document says "Approved for Implementation" while `SPEC.md` still contains unresolved product decisions that affect data and workflows. | Use `Draft / Conditional` status and isolate unresolved decisions in versioned configuration. |
| V-02 | High | "Hybrid Microservices Architecture" is ambiguous because the commerce backend is a modular monolith. | Describe the system as a modular monolith plus independently deployed agent service and managed infrastructure. |
| V-03 | High | Optimistic locking alone does not guarantee that concurrent reservations cannot oversell. | Use an atomic conditional reservation update in one transaction, with versioning and reservation expiry. |
| V-04 | High | PostGIS is deferred to Phase 2 even though Phase 1 requires delivery zones, pickup radius, distance, and route feasibility. | Enable PostGIS or an equivalent geospatial provider in Phase 1. |
| V-05 | High | Spring in-process application events are presented as an alternative to a durable event bus. They do not survive process or transaction failure. | Use a transactional outbox and durable queue/event bus; allow in-process handlers only for non-critical local work. |
| V-06 | High | Parent-order and vendor-order transition rules are mentioned but not completely specified. | Define state ownership, guarded transitions, terminal states, actor/reason metadata, and idempotent duplicates. |
| V-07 | High | Multi-vendor payment authorization, partial vendor rejection, capture adjustment, transfer timing, refunds, and webhook reconciliation are incomplete. | Define the payment saga and ledger snapshots; keep transfers after capture and policy-defined delivery/settlement gates. |
| V-08 | High | Agent confirmation is described only for financial mutations and conflicts with role-specific approval requirements. | Persist scoped, expiring approval records for all consequential actions and validate them server-side. |
| V-09 | High | Dispatch does not define atomic driver assignment, offer expiry, pickup verification, reassignment, or combined-delivery completion. | Add offer and delivery state contracts with conditional assignment and per-stop verification. |
| V-10 | Medium | The API section names route groups but not error format, idempotency behavior, concurrency headers, pagination, or authorization boundaries. | Define cross-cutting API conventions and key endpoint contracts. |
| V-11 | Medium | Messaging, notifications, support cases, refunds, vendor announcements/offers, and operator workflows are missing. | Add module responsibilities, entities, APIs, events, and manual fallback paths. |
| V-12 | Medium | Audit logging includes raw arguments, which could retain secrets or unnecessary personal data. | Store redacted summaries and hashes, approval linkage, correlation IDs, and configured retention. |
| V-13 | Medium | "Sanitize input" and prompting alone are insufficient prompt-injection controls. | Treat retrieved/uploaded text as data, isolate it from instructions, constrain tools, validate output, and authorize every action. |
| V-14 | Medium | Performance targets differ from `SPEC.md` without capacity assumptions or an approved requirement change. | Retain authoritative targets and optionally track stricter internal objectives separately. |
| V-15 | Medium | No deployment rollback, disaster recovery, observability, alerting, migration, or secret-rotation strategy is defined. | Add operational readiness requirements and measurable recovery objectives. |
| V-16 | Medium | WCAG 2.2 AA and non-voice/manual fallback acceptance requirements are absent. | Add accessibility and agent-degraded-mode requirements to UI and test strategy. |
| V-17 | Medium | No requirements-to-tests traceability exists. | Add a verification matrix tied to the MVP acceptance criteria. |

## 4. Coverage Assessment

| `SPEC.md` area | Draft coverage | Revised disposition |
| --- | --- | --- |
| Roles and agent model | Partial | Role scopes, approval artifacts, memory controls, and manual fallback added |
| Multi-vendor ordering | Partial | Parent/child transactions, quote snapshots, rejection policy, and saga added |
| Inventory and CSV | Partial | Atomic reservation, expiry, import staging, commit, rollback, and concurrency added |
| Order state model | Partial | Complete guarded state ownership and transition behavior added |
| Payments and UCP/SPT | Partial | Provider abstraction, fallback, webhooks, capture, transfers, and refunds added |
| Dispatch and delivery | Minimal | Offer lifecycle, atomic assignment, stops, proof, and reassignment added |
| Messaging/support | Missing | Modules, privacy boundaries, cases, evidence, and escalation added |
| Announcements/offers | Missing | Phase 1 manual publishing workflow added |
| Security/governance | Partial | Threat controls, approval validation, redaction, consent, and emergency disable added |
| Non-functional requirements | Partial | Reliability, privacy, accessibility, observability, DR, and SLOs added |
| API/events/data | Partial | Conventions, core contracts, outbox semantics, and entity constraints added |
| Acceptance traceability | Missing | Verification matrix added |

## 5. Decisions That Still Require Product Approval

Implementation can begin on bounded modules, but production checkout and dispatch policy cannot be finalized until these values are approved:

- Launch jurisdiction and applicable tax, labor, privacy, and delivery rules.
- Initial platform fee `X` and whether it varies by market, category, or agreement.
- Delivery fee, minimum order, driver payout, and settlement formulas.
- Maximum vendors, pickup radius, route duration, and freshness limits.
- Behavior when one vendor rejects: continue partially, replace vendor, or cancel all.
- Payment capture and cancellation timing.
- Whether customer auto-order and driver auto-accept ship in MVP.
- Automated refund limits and proof-of-delivery thresholds.
- Retention periods and selected maps, messaging, identity, and model providers.

These are represented in `TECH_SPEC.md` as versioned configuration or explicit decision records rather than hardcoded assumptions.

## 6. Validation Conclusion

The original draft is **conditionally valid as an architecture proposal** but **fails implementation-readiness validation**. The revised technical specification is suitable for engineering estimation and detailed design, subject to the open product decisions above and review by security, payments, operations, and legal stakeholders.
