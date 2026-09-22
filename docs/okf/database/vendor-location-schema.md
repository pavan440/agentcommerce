---
type: Database Schema
title: Vendor and location schema
description: Vendor organization, membership, location, hours, service area, and commerce-setting schema.
tags: [database, vendor, location, postgis, tenancy]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: vendor-schema-v3
    resource: ../../database/VENDOR_SCHEMA_DESIGN_V3.md
    title: Vendor schema design V3
  - id: migration-v2
    resource: ../../../domain-api/src/main/resources/db/migration/V2__create_vendor_schema.sql
    title: Vendor migration
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| `vendors` | Merchant storefront organization identity, category, branding, currency, status, fee tier reference |
| `vendor_memberships` | User access to vendor organizations and vendor roles |
| `vendor_membership_locations` | Optional location scoping for staff and managers |
| `vendor_invitations` | Invitation lifecycle before membership acceptance |
| `vendor_locations` | Physical store identity, address, PostGIS point, handoff instructions, status |
| `vendor_location_hours` | Recurring weekly operating windows |
| `vendor_location_special_hours` | Holiday and event overrides |
| `vendor_service_areas` | Delivery eligibility polygons |
| `vendor_location_commerce_settings` | Prep times, busy mode padding, pause, fulfillment toggles, notification channel, fees, tax mode |

# Generation Rules

- Vendor/location authorization must require active membership and location scope where applicable.
- Creating a vendor must create an owner membership in the same transaction.
- The final active owner cannot be removed or demoted.
- `CLOSED` and `SUSPENDED` states must preserve historical commerce records.
- Platform fee rate `X` is not mutable vendor commerce setting data; quote logic snapshots it from fee configuration or agreements.