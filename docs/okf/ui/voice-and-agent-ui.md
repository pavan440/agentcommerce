---
type: UI Roadmap
title: Voice and agent UI transition
description: Phase 1 text-agent UI and Phase 2 real-time voice multi-modal UI expectations.
tags: [ui, voice, agents, roadmap, multimodal]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: ui-design
    resource: ../../ui/PORTALS_UI_AND_VOICE_AGENT_DESIGN.md
    title: Portals UI and voice agent design
  - id: agent-capability
    resource: ../../architecture/AGENT_NATIVE_MARKETPLACE_CAPABILITY_SPEC.md
    title: Agent-native marketplace capability spec
  - id: hitl-spec
    resource: ../../architecture/AGENT_PREFERENCE_ROUTING_AND_HITL_SPEC.md
    title: Agent preference routing and HITL spec
---
# Phase 1

- Natural-language text chat and visual widgets.
- Push-to-talk speech input where browser/device support permits.
- Optional text-to-speech responses.
- Inline rich action cards and click-to-approve confirmation modals.
- Manual forms and grids for every consequential workflow.

# Phase 2

- Low-latency bidirectional voice with interruption support.
- Voice-driven visual canvas synchronization.
- Voice state components such as orb, waveform, transcript, tool timeline, and synchronized highlights.
- Hands-free modes for driver and kitchen workflows.

# Generation Rules

- Every voice action must have equivalent visual confirmation and manual workflow.
- Voice cannot bypass approval, policy, price, safety, or authorization boundaries.
- Consequential voice commands must bind to the same approval and tool execution flow as text/manual commands.
- Do not implement Phase 2 real-time voice UI until the protected agent-tool adapters and safety checks exist.