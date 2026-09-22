---
type: UI Surface
title: Current portal implementation
description: Implemented dependency-free responsive PWA portal routes and workspaces.
tags: [ui, pwa, portal, implementation]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: marketplace-portal
    resource: ../../ui/MARKETPLACE_PORTAL.md
    title: Marketplace portal documentation
  - id: portal-app
    resource: ../../../portal/app.js
    title: REST validation portal script
  - id: role-app
    resource: ../../../portal/role-app.js
    title: Role PWA script
---
# Implemented Routes

| Route | Purpose |
| --- | --- |
| `/` | REST validation portal and implemented workflow testing |
| `/customer/` | Customer role PWA shell and implemented customer workflows |
| `/vendor/` | Vendor role PWA shell and implemented vendor/inventory workflows |
| `/dasher/` | Dasher role PWA shell and implemented dasher workflows |
| `/admin/` | Admin/operator role PWA shell and implemented operator workflows |

# Current Authentication

- Users provide the Domain API base URL and a real bearer JWT.
- Tokens are stored only in browser `sessionStorage`.
- No header-based development bypass, role escalation shortcut, or fake-auth mode exists.

# Generation Rules

- Keep portal behavior aligned with production authentication assumptions.
- Do not add UI-only business rules that bypass backend validation.
- Preserve PWA installability and responsive behavior for the four role apps.
- When a future native app is introduced, share API contracts and design tokens instead of forking business behavior.