# Agent-to-Agent Preference Routing & Human-in-the-Loop (HITL) Specification

**Version:** 1.0  
**Status:** Approved Specification  
**Subsystems:** Customer Agent, Vendor Agent, Fallback Preference Cascades, Autonomous Rerouting, Human-in-the-Loop (HITL) Gate  

---

## 1. Executive Summary

This document specifies the autonomous negotiation and preference routing protocol between the **Customer Agent** and **Vendor Agent**, including:
1. **Customer Fallback Preferences**: Multi-level fallback chains (e.g. *Dish: "Spicy Biryani" → Fallback: "Samosa Chaat"*; *Vendor: "Paradise Bistro" → Fallback: "Curry Express"*).
2. **Vendor Agent Acceptance Policies**: Vendor operational thresholds (kitchen backlog limits, automated out-of-stock substitution proposals).
3. **Autonomous Execution vs HITL Approval Gate**:
   - **Autonomous Path**: If a fallback exactly matches the customer's saved pre-approved preference rules within price tolerance, the Customer Agent automatically reroutes/substitutes.
   - **Human-in-the-Loop (HITL) Path**: If no matching rule exists, price exceeds tolerance, or allergens are involved, the agent creates an expiring `agent_approvals` token and prompts the customer for 1-tap/voice confirmation.

---

## 2. Multi-Agent Interaction Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant CA as Customer Agent
    participant OS as Order & Quote Service
    participant VA as Vendor Agent (Rest 1)
    participant VA2 as Vendor Agent (Rest 2)
    participant HITL as Human-in-the-Loop Gate

    Customer->>CA: "I want spicy Chicken Biryani"
    CA->>CA: Inspects Customer Preferences (Fallback: Samosa Chaat, Vendor 2: Curry Express)
    CA->>OS: Create Pending Order (Primary: Rest 1 - Biryani)
    OS->>VA: Dispatch Sub-Order to Vendor 1
    
    alt Vendor 1 Accepts
        VA-->>OS: Accept Order (Prep time: 20 mins)
        OS-->>Customer: Order Confirmed & In Preparation
    else Vendor 1 Rejects / 86'd (Biryani Sold Out)
        VA-->>OS: Reject (Reason: "Biryani Out of Stock", Propose: Samosa Chaat)
        OS-->>CA: Sub-Order Rejected with Alternative
        
        alt Preference Rule Matched & Pre-Approved
            Note over CA: Customer Rule: "Auto-Reroute to Rest 2 if Biryani available"
            CA->>OS: Reroute Sub-Order to Restaurant 2
            OS->>VA2: Dispatch to Vendor 2
            VA2-->>OS: Accept Order (Prep time: 18 mins)
            OS-->>Customer: "Vendor 1 was out of Biryani; auto-rerouted to Vendor 2 as per your preference!"
        else Preference Unmatched or Exceeds Price Threshold
            CA->>HITL: Create Pending Approval Token
            HITL-->>Customer: "Vendor 1 is out of Biryani. Substitute Samosa Chaat ($8.99, refund $5.00) or order from Vendor 2 ($14.99)?"
            Customer->>HITL: Confirms "Order from Vendor 2"
            HITL->>OS: Commit Reroute to Vendor 2
            OS->>VA2: Dispatch to Vendor 2
            VA2-->>OS: Accept Order
        end
    end
```

---

## 3. Data Structure: Customer Preference & Fallback Rules

```json
{
  "user_id": "usr_9842",
  "category": "RESTAURANT",
  "spice_preference": "EXTRA_SPICY",
  "dietary_flags": ["HALAL", "NUT_FREE"],
  "item_fallback_chains": [
    {
      "primary_item_query": "Chicken Biryani",
      "acceptable_fallbacks": [
        { "item_name": "Mutton Biryani", "max_price_delta_minor": 300, "auto_approve": true },
        { "item_name": "Samosa Chaat", "max_price_delta_minor": 0, "auto_approve": true }
      ]
    }
  ],
  "vendor_priority_chains": [
    {
      "cuisine": "INDIAN",
      "primary_vendor_id": "ven_paradise_downtown",
      "fallback_vendor_ids": ["ven_curry_express_west", "ven_spice_junction"]
    }
  ],
  "auto_reroute_policy": {
    "enabled": true,
    "max_delivery_time_delta_minutes": 15,
    "max_total_price_delta_minor": 400
  }
}
```

---

## 4. Vendor Agent Operational Rules

Vendors configure automated decision boundaries in `vendor_location_commerce_settings`:

```json
{
  "vendor_location_id": "loc_104",
  "auto_accept_rules": {
    "enabled": true,
    "max_active_orders_queue": 12,
    "max_prep_backlog_minutes": 30,
    "require_exact_inventory_match": true
  },
  "auto_substitution_proposals": [
    {
      "out_of_stock_item_id": "prod_biryani_chicken",
      "recommended_substitute_id": "prod_biryani_lamb",
      "price_adjustment_type": "MATCH_ORIGINAL_PRICE"
    }
  ]
}
```

---

## 5. Human-in-the-Loop (HITL) Execution Rules

The system triggers an `agent_approvals` token and halts autonomous execution whenever:
1. **Price Delta Exceeded**: Proposed substitution increases order total by more than the customer's configured tolerance (e.g. `>$4.00`).
2. **Allergen / Dietary Ambiguity**: An alternative product does not explicitly certify customer allergen flags (e.g. `NUT_FREE`).
3. **Delivery Delay Surge**: Rerouting to Vendor 2 increases estimated delivery arrival by >15 minutes.
4. **Primary vs Fallback Disparity**: No matching entry in `item_fallback_chains`.

When triggered, the customer receives an instant **1-Tap Interactive Push Notification / Voice Prompt**:
- *"Paradise Bistro is sold out of Chicken Biryani. Substitute with Samosa Chaat (-$5.00 refund) or Reroute to Curry Express (+$2.00)?"*
- Buttons: `[ Samosa Chaat ]` `[ Reroute Curry Express ]` `[ Cancel Order ]`

---

## 6. Pre-Purchase Inquiry and Offer Negotiation

The customer may begin with voice or text requests such as:

- "Show me this restaurant's menu."
- "Which nearby restaurant has a discount?"
- "Ask this restaurant whether they provide tiffin service."
- "If I order every day, can they offer a repeat-customer discount?"

The Customer Agent resolves these requests as follows:

1. Read published menus, current availability, eligible offers, fees, and service metadata through authoritative APIs.
2. Answer published facts immediately and identify the source and validity of any discount.
3. For an unpublished service or requested deal, create a structured, auditable vendor inquiry containing the customer's question and only the minimum information authorized for disclosure.
4. The Vendor Agent answers automatically only from vendor-approved facts, service definitions, or response policies.
5. A vendor owner approves any new custom price, discount, recurring cadence, subscription-like commitment, or exception not already covered by policy.
6. The Vendor Agent returns a structured, expiring proposal. The Customer Agent explains price, cadence, minimum commitment, cancellation terms, delivery assumptions, and expiry.
7. Inquiry and negotiation do not create an order, recurring charge, or subscription. The customer must explicitly accept the proposal and authorize any resulting commerce action.
8. All messages, proposals, approvals, expirations, and resulting actions retain correlation and audit identifiers.

Agents communicate through authenticated domain APIs and events rather than unrestricted direct model-to-model messaging. Structured services remain authoritative for menu, offer, order, and payment state.