---
type: Database Schema
title: Agent governance, memory, and safety schema
description: Agent conversations, messages, memories, approvals, policies, actions, and killswitch database model.
tags: [database, agents, memory, approvals, audit, safety]
status: draft
generated: { by: codex/gpt-5, at: 2026-09-22T00:00:00-07:00 }
sources:
  - id: agent-schema
    resource: ../../database/AGENT_SCHEMA_DESIGN_V1.md
    title: Agent governance schema design
  - id: migration-v4
    resource: ../../../domain-api/src/main/resources/db/migration/V4__create_agent_schema.sql
    title: Agent migration
---
# Owned Concepts

| Table group | Responsibility |
| --- | --- |
| `agent_conversations` | Role-scoped conversation sessions and retention state |
| `agent_messages` | Message history, tool call/result payloads, prompt-injection telemetry |
| `agent_memories` | User-approved long-term memory and embeddings |
| `agent_approvals` | Expiring human-in-the-loop approvals for consequential actions |
| `agent_policies` | User-configured autonomy boundaries and monetary limits |
| `agent_actions` | Append-only audit of tool executions and policy outcomes |
| `agent_killswitches` | Emergency disable controls by global, role, tool, or tenant target |

# Generation Rules

- Agent memory must be consented, inspectable, correctable, exportable, and revocable.
- Consequential tool calls must bind approval to exact normalized arguments or argument hash.
- Agent action logs must redact sensitive inputs and preserve correlation IDs.
- Killswitches must be checked by tool execution paths, not only by UI.
- The schema supports agents; it does not authorize direct agent writes to commerce tables.