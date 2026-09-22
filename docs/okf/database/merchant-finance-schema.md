---
type: Database Schema
title: Merchant finance and legal schema
description: Legal merchant, Stripe Connect, fee agreement, payout schedule, and bank metadata schema.
tags: [database, merchant, stripe, payments, settlement]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: merchant-schema
    resource: ../../database/MERCHANT_SCHEMA_DESIGN_V1.md
    title: Merchant financial and legal schema design
  - id: migration-v3
    resource: ../../../domain-api/src/main/resources/db/migration/V3__create_merchant_schema.sql
    title: Merchant migration
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| `merchants` | Legal entity, DBA, tax verification state, business status |
| `merchant_stripe_accounts` | Stripe Connect mapping, charge/payout capability, onboarding state |
| `merchant_fee_agreements` | Platform fee `X` basis points, fixed fees, effective intervals |
| `merchant_payout_schedules` | Settlement frequency, payout delays, thresholds |
| `merchant_bank_accounts` | Masked settlement bank metadata for reporting and verification |

# Generation Rules

- Store only masked or provider-tokenized financial identifiers in application tables.
- Fee agreements must be effective-dated and quote/order flows must snapshot the selected agreement.
- Payment provider state must be reconciled from signed webhooks, not trusted solely from synchronous API responses.
- Closing or suspending merchant records must not erase vendor/order/payment history.