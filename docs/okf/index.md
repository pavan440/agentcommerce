# AgentCommerce OKF bundle

This directory is the Open Knowledge Format bundle for AgentCommerce. It is a progressive-disclosure layer over the larger product, technical, backend, architecture, database, UI, and audit documents.

## Start here

- [Project overview](/project/agentcommerce.md) - what AgentCommerce is and which source documents govern it.
- [Code generation rules](/project/code-generation-rules.md) - how agents should generate code from this repository knowledge.
- [System boundaries](/architecture/system-boundaries.md) - mandatory trust, persistence, and agent execution boundaries.
- [Domain map](/domains/domain-map.md) - domain modules, ownership, and implemented surface.
- [Current implementation status](/implementation/current-status.md) - implemented foundations and active gaps.
- [Gap priority](/implementation/gap-priority.md) - recommended build order.

## Database concepts

- [Migration map](/database/migration-map.md) - executable Flyway migration order and ownership.
- [Identity, customer, and driver schema](/database/identity-and-access-schema.md) - users, profiles, addresses, drivers, devices, consents.
- [Vendor and location schema](/database/vendor-location-schema.md) - vendors, memberships, locations, hours, service areas, commerce settings.
- [Merchant finance and legal schema](/database/merchant-finance-schema.md) - merchants, Stripe Connect, fees, payouts, bank metadata.
- [Agent governance schema](/database/agent-governance-schema.md) - memory, approvals, policies, actions, killswitches.
- [Catalog and inventory schema](/database/catalog-inventory-schema.md) - catalog items, inventory records, CSV imports, staged rows.
- [POS, promotions, and community schema](/database/pos-promotions-schema.md) - POS, sales intelligence, promotions, announcements.

## API concepts

- [Inventory API](/apis/inventory-api.md) - current inventory and customer menu API slice.

## UI concepts

- [Current portal implementation](/ui/portal-implementation.md) - shipped PWA routes, authentication, and implemented workspaces.
- [Role workspace map](/ui/role-workspaces.md) - current and target responsibilities by role.
- [Shared design system](/ui/design-system.md) - tokens, responsive behavior, accessibility, shared UI rules.
- [Voice and agent UI transition](/ui/voice-and-agent-ui.md) - Phase 1 text-agent and Phase 2 voice/multimodal expectations.

## Source documents

- [Product specification](../../SPEC.md)
- [Technical specification](../../TECH_SPEC.md)
- [Functionality catalog](../FUNCTIONALITY_CATALOG.md)
- [Implementation gap report](../IMPLEMENTATION_GAP_REPORT.md)
- [Backend docs](../backend/)
- [Architecture docs](../architecture/)
- [Database docs](../database/)
- [UI docs](../ui/)