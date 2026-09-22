---
type: Database Schema
title: Identity, customer, and driver schema
description: User, customer, driver, device, safety contact, and consent data owned by the identity domain.
tags: [database, identity, customer, driver, consent]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: user-schema
    resource: ../../database/USER_SCHEMA_DESIGN_V1.md
    title: User and identity schema design
  - id: migration-v1
    resource: ../../../domain-api/src/main/resources/db/migration/V1__create_identity_schema.sql
    title: Identity migration
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| Users and identities | Core account entity, OIDC/OAuth identity links, status, immutable provider claims |
| Roles | Global roles such as `CUSTOMER`, `VENDOR_MEMBER`, `DRIVER`, and `OPERATOR` |
| Customer profile | Preferences, dietary restrictions, substitution mode, default tip and bounded autonomy data |
| Customer addresses | Saved delivery destinations, PostGIS coordinates, instructions, default flag |
| Driver profile and zones | Vehicle/onboarding state, online availability, ratings, PostGIS operating zones |
| Device tokens | Push notification registration data |
| Emergency contacts | Safety contact records |
| Consents | Location tracking and agent personalization consent history |

# Generation Rules

- Do not store credentials, refresh tokens, payment instruments, or provider secrets in profile tables.
- Treat consent as versioned and revocable; revocation stops future collection.
- Driver online state must remain subject to verification and eligibility checks.
- Address and zone coordinates must be validated before persistence.
- User-facing export/delete behavior must honor legal and financial retention exceptions explicitly.