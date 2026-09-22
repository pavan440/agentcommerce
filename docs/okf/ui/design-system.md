---
type: UI Design System
title: Shared portal design system
description: Shared portal design tokens, typography, accessibility, and responsive UI principles.
tags: [ui, design-system, tokens, accessibility]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: ui-design
    resource: ../../ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md
    title: Portals UI and voice agent design
  - id: role-css
    resource: ../../../portal/role-app.css
    title: Role app stylesheet
  - id: portal-css
    resource: ../../../portal/styles.css
    title: Portal stylesheet
---
# Shared Principles

- Four role applications share design tokens, authentication patterns, API contracts, accessibility primitives, agent components, and observability patterns.
- Shared packages must not create cross-role data access.
- UI must remain responsive across mobile, tablet, and desktop where each role requires it.
- Manual forms and grids remain available when agent functionality is unavailable.

# Token Direction

The approved UI design defines dark/light tokens for canvas, surface, elevated surface, primary emerald, agent cyan, amber warnings, rose danger, text, and subtle borders. Current implementation uses simpler dependency-free CSS and should evolve without breaking PWA routes.

# Generation Rules

- Prefer existing CSS/component patterns in `portal/` before inventing a new design layer.
- Keep forms, tables, status chips, and approval controls legible under mobile constraints.
- Use explicit loading, error, empty, and unauthorized states for API-backed screens.
- Agent UI components must never hide the manual workflow required to complete the same action.