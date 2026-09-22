---
type: UI Surface
title: Role workspace map
description: Customer, vendor, dasher, admin, and REST Lab workspace responsibilities.
tags: [ui, roles, customer, vendor, dasher, admin]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: marketplace-portal
    resource: ../../ui/MARKETPLACE_PORTAL.md
    title: Marketplace portal documentation
  - id: ui-design
    resource: ../../ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md
    title: Portals UI and voice agent design
---
# Current Workspace Responsibilities

| Workspace | Current implemented responsibility |
| --- | --- |
| Customer | Load/update profile preferences, manage delivery addresses, grant/revoke consents |
| Vendor | Create draft vendor, load vendor portfolio, create locations, manage commerce settings, inventory upload/display in role PWA |
| Dasher | Start onboarding, update vehicle details, view verification/availability, manage zones, go online/offline when eligible |
| Admin | Submit versioned dasher verification decisions for authenticated operators |
| REST Lab | Send arbitrary API requests with status, latency, and formatted JSON responses |

# Target Workspace Direction

| Workspace | Target role experience |
| --- | --- |
| Customer | Marketplace discovery, multi-vendor cart, transparent quote, order tracking, customer agent workspace |
| Vendor | Storefront/menu studio, kitchen tablet, inventory, orders, offers, inquiries, vendor agent workspace |
| Dasher | Courier command, offers, route progress, earnings, safety, driver agent workspace |
| Admin | Dense operations console for verification, support, safety, disputes, and platform health |

# Generation Rules

- Add manual workflow first or alongside agent workflow.
- Keep each role application behind role-appropriate data boundaries.
- Use the same API response shape as backend documentation; do not invent frontend-only contracts.
- Persist only session-local auth state unless a later security design approves otherwise.