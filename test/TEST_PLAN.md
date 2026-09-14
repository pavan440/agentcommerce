# Test Plan for Agentic Community Delivery Marketplace

## Overview
This document outlines test cases for validating the core functionalities defined in SPEC.md.

## Test Categories

### 1. Customer Ordering Flow (SPEC Section 6.1)

#### TC-CO-001: Customer Agent Product Search
**Objective:** Verify customer agent can search active local vendors and orderable inventory
**Steps:**
1. Customer requests products/vendors via natural language
2. Customer agent queries catalog service
3. Verify only active vendors with available inventory are returned
4. Verify results include base prices, platform fee (X%), delivery costs, ETAs
**Expected:** Filtered results matching customer query with complete pricing info

#### TC-CO-002: Multi-Vendor Cart Creation
**Objective:** Verify cart can contain items from multiple vendors
**Steps:**
1. Customer adds items from Vendor A
2. Customer adds items from Vendor B
3. Cart service groups items by vendor location
4. Validate coordinated delivery constraints (max vendors, pickup radius, route duration)
**Expected:** Cart contains vendor sub-orders with validation of delivery feasibility

#### TC-CO-003: Quote Calculation with Platform Fee
**Objective:** Verify quote service correctly calculates all price components
**Steps:**
1. Create cart with items from multiple vendors
2. Request quote
3. Verify calculation: product_platform_fee = in_store_price * (X/100)
4. Verify: customer_price = in_store_price + product_platform_fee
5. Verify quote shows: base total, platform fee, delivery fee, taxes, tip, discounts, final total
**Expected:** Accurate breakdown with configurable X% fee (not hardcoded)

#### TC-CO-004: Order Creation with Inventory Reservation
**Steps:**
1. Customer confirms quote
2. Order service creates parent order + vendor sub-orders
3. Inventory reserved per vendor
4. Payment authorized for complete order
5. Verify idempotency with repeated requests
**Expected:** Atomic order creation with short-lived inventory reservation

#### TC-CO-005: Partial Vendor Rejection Handling
**Steps:**
1. Create multi-vendor order
2. Vendor A accepts, Vendor B rejects
3. Verify platform applies partial-rejection policy
4. Verify payment adjustment/refund for rejected portion
**Expected:** Order state becomes `partially_accepted` with appropriate refund

---

### 2. Vendor Fulfillment Flow (SPEC Section 6.2)

#### TC-VF-001: Vendor Receives Sub-Order Only
**Objective:** Verify vendor sees only their products
**Steps:**
1. Create multi-vendor order
2. Vendor A views order details
3. Verify only Vendor A's products visible
**Expected:** Vendor cannot see other vendors' items

#### TC-VF-002: Vendor Acceptance with Preparation Time
**Steps:**
1. Vendor receives new sub-order
2. Vendor agent summarizes items, notes, delivery time
3. Vendor accepts with preparation estimate
4. Verify inventory changes from reserved to committed
**Expected:** Order state changes to `accepted`, inventory committed

#### TC-VF-003: Vendor Rejection Flow
**Steps:**
1. Vendor rejects order with reason
2. Verify inventory reservation released
3. Verify customer notified
4. Verify payment authorization adjusted
**Expected:** Order state `rejected`, inventory freed, customer refunded

#### TC-VF-004: Pickup Verification
**Steps:**
1. Vendor marks order ready for pickup
2. Driver arrives and provides verification code
3. Vendor confirms handoff
4. Verify sub-order state changes to `picked_up`
**Expected:** Secure handoff with state transition

---

### 3. Driver Delivery Flow (SPEC Section 6.3)

#### TC-DD-001: Coordinated Delivery Offer
**Objective:** Verify driver receives multi-pickup delivery offer
**Steps:**
1. Dispatch confirms accepted sub-orders meet distance/timing limits
2. Driver receives offer showing: all pickups, order, distance, time, payout
3. Verify offer expires after configured window
**Expected:** Complete delivery route information with expiration

#### TC-DD-002: Multi-Pickup Confirmation
**Steps:**
1. Driver accepts offer
2. Driver confirms arrival at each vendor
3. Driver verifies and confirms each pickup separately
4. Verify state tracking per pickup
**Expected:** Each pickup individually confirmed before proceeding

#### TC-DD-003: Combined Delivery Completion
**Steps:**
1. Driver collects all accepted sub-orders
2. Driver travels to customer
3. Driver captures required proof of delivery
4. Driver confirms combined delivery
5. Verify payout finalized
**Expected:** Single delivery confirmation triggers completion

---

### 4. Substitution Flow (SPEC Section 6.4)

#### TC-SUB-001: Preference-Based Substitution
**Steps:**
1. Customer sets substitution mode to "preference-based"
2. Item becomes unavailable
3. Vendor proposes substitute
4. Customer agent auto-accepts if matches saved preferences
**Expected:** Automatic substitution within preference bounds

#### TC-SUB-002: Approval-Required Substitution
**Steps:**
1. Customer sets mode to "approval-required"
2. Item unavailable, vendor proposes substitute
3. Customer agent recommends but requires approval
4. Customer approves/rejects
5. Verify payment adjustment if price differs
**Expected:** Manual approval required before accepting

#### TC-SUB-003: Substitution Timeout
**Steps:**
1. Item unavailable, no acceptable substitute
2. Timeout expires
3. Verify item removed and refunded per policy
**Expected:** Automatic refund after timeout

---

### 5. Inventory Management (SPEC Section 7)

#### TC-INV-001: Form-Based Update with Concurrency
**Steps:**
1. Vendor edits inventory quantity via form
2. Simultaneous edit by another user
3. Verify optimistic concurrency protection
4. Verify audit history recorded
**Expected:** Second save fails with version conflict, audit log updated

#### TC-INV-002: CSV Import Validation
**Steps:**
1. Upload CSV with valid and invalid rows
2. Verify validation summary shows: creates, updates, deactivations, errors
3. Verify import not applied until vendor confirms
4. Commit with errors excluded
**Expected:** Row-level errors identified, atomic commit of valid rows only

#### TC-INV-003: CSV Schema Validation
**Test Data:**
```csv
sku,name,price,quantity_on_hand,is_available,operation
SKU-001,Test Item,9.99,10,true,upsert
SKU-002,,5.00,5,true,upsert  # Missing name
SKU-003,Item,-1.00,10,true,upsert  # Negative price
SKU-004,Item,10.999,5,true,upsert  # 3 decimal places
SKU-001,Duplicate,5.00,5,true,upsert  # Duplicate SKU
```
**Expected:** Row-level errors for malformed data

#### TC-INV-004: Inventory Reservation Expiration
**Steps:**
1. Add item to cart (reserves inventory)
2. Wait for reservation timeout
3. Verify inventory released
4. Verify item available for other customers
**Expected:** Automatic release on timeout

#### TC-INV-005: Checkout with Insufficient Inventory
**Steps:**
1. Item has quantity_available = 5
2. Customer tries to add 6 to cart
3. Verify rejection with insufficient quantity message
**Expected:** Cannot exceed available quantity

---

### 6. Order State Model (SPEC Section 8)

#### TC-OSM-001: Valid State Transitions
**Test valid transitions:**
- draft → quoted → inventory_reserved → payment_authorized → submitted
- submitted → partially_accepted OR accepted
- accepted → driver_assigned → pickups_in_progress → out_for_delivery → delivered
**Expected:** All valid transitions succeed with audit record

#### TC-OSM-002: Invalid State Transitions
**Test invalid transitions:**
- draft → delivered (skip intermediate states)
- delivered → submitted (reverse terminal state)
- canceled → accepted (terminal state transition)
**Expected:** Rejected with current state returned safely, no side effects

#### TC-OSM-003: Idempotent Transition Requests
**Steps:**
1. Submit same transition request twice
2. Verify second request returns current state
3. Verify no duplicate side effects (no double-charges, no duplicate events)
**Expected:** Safe idempotent behavior

#### TC-OSM-004: Parent Order Acceptance Logic
**Steps:**
1. Create order with 3 vendor sub-orders
2. All 3 accept → parent becomes `accepted`
3. Only 2 accept → parent becomes `partially_accepted`
**Expected:** Correct parent state based on sub-order acceptance

---

### 7. Agent Model (SPEC Section 5)

#### TC-AGT-001: Agent Identity Propagation
**Steps:**
1. Customer agent performs action
2. Verify delegating user's identity, role, consent carried into action
3. Verify audit log includes agent and human identities
**Expected:** Full identity chain in audit trail

#### TC-AGT-002: Agent Approval Requirements
**Test cases requiring approval:**
- Placing order without auto-order policy
- Substitution increasing total beyond tolerance
- Address change after checkout
- Bulk inventory changes above threshold
- Price changes above configured percentage
**Expected:** Confirmation requested before execution

#### TC-AGT-003: Agent Communication Protocol
**Steps:**
1. Customer agent sends inquiry to vendor
2. Vendor agent responds with structured proposal
3. Verify communication through domain APIs/events only
4. Verify free-form messages cannot commit business state
**Expected:** Structured, auditable inter-agent communication

#### TC-AGT-004: Agent Privacy Protection
**Steps:**
1. Customer agent queries vendor details
2. Verify private contact/payment data not exposed
3. Driver agent contacts customer
4. Verify sanitized contact method used
**Expected:** No private data leakage between parties

---

### 8. Business Model & Pricing (SPEC Section 3.3)

#### TC-BM-001: Configurable Platform Fee
**Steps:**
1. Set platform fee X = 15%
2. Create product with in_store_price = $10.00
3. Verify product_platform_fee = $1.50
4. Verify customer_price = $11.50
5. Change X to 20%
6. Verify existing orders retain original X value
7. Verify new quotes use updated X
**Expected:** X stored as configuration, order snapshots immutable

#### TC-BM-002: Price Snapshot Immutability
**Steps:**
1. Create order with specific X value
2. Change X configuration
3. Verify order total unchanged
4. Verify refund uses original order values
**Expected:** Historical orders never recalculated

---

## Integration Test Scenarios

### INT-001: End-to-End Happy Path
1. Customer searches for food
2. Adds items from 2 vendors to cart
3. Reviews quote with fee breakdown
4. Places order with payment authorization
5. Both vendors accept with preparation times
6. Driver accepts coordinated delivery
7. Driver completes both pickups
8. Driver delivers to customer
9. Payment captured, order completed

### INT-002: Partial Failure Recovery
1. Customer orders from 3 vendors
2. One vendor rejects
3. Partial refund processed
4. Remaining vendors fulfill
5. Driver collects from accepting vendors only
6. Delivery completed with partial order

### INT-003: Inventory Race Condition
1. Two customers view same low-stock item
2. Both attempt to add to cart simultaneously
3. First succeeds, second fails with availability error
4. First customer completes purchase
5. Inventory correctly decremented

---

## Performance Requirements

- Inventory view updates within 10 seconds of changes
- Quote calculation < 2 seconds
- Order placement < 5 seconds
- Agent response time < 3 seconds for standard queries

## Security Requirements

- All agent actions authenticated and authorized
- PCI-compliant payment handling (no raw card storage)
- Audit logging for all agent decisions and tool calls
- Input validation for chat text, CSV, external content
