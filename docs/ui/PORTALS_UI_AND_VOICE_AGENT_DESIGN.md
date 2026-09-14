# Portals UI/UX Architecture & Voice Agent System Design

**Version:** 1.0  
**Status:** Approved for Implementation  
**Phases:** Phase 1 (Natural Language Conversational UI) & Phase 2 (Real-Time Voice AI Agent Multi-Modal Transition)  
**Target Systems:** Admin Operations Console, Vendor Kitchen/Store Portal, Customer Marketplace, Driver Courier Interface  

---

## 1. Executive Summary

This document defines the complete UI/UX architecture, component hierarchy, design system tokens, and interaction flows across the four role applications. It establishes a unified multi-modal interaction paradigm that seamlessly transitions from **Phase 1 (Natural Language Text Chat & Visual Widgets)** to **Phase 2 (Real-Time Bidirectional Voice Agents with Synchronized Visual Canvas)**.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           MULTI-MODAL INTERACTION EVOLUTION                     │
├────────────────────────────────────────┬────────────────────────────────────────┤
│ PHASE 1: NATURAL LANGUAGE TEXT (MVP)   │ PHASE 2: REAL-TIME VOICE-FIRST AI      │
├────────────────────────────────────────┼────────────────────────────────────────┤
│ • Natural Language Chat Dock & Drawer  │ • Low-latency WebRTC Voice Streaming   │
│ • Inline Rich Action & Tool Cards      │ • Animated Glowing Voice Orb & Waveform│
│ • Click-to-Approve Confirmation Modals │ • Live Audio Barge-In / Interruption   │
│ • Manual Form & Grid Fallbacks         │ • Voice-Driven Visual Canvas Sync      │
│ • Keyboard / Screen Accessibility      │ • Hands-Free Eyes-Free Driver/Kitchen  │
└────────────────────────────────────────┴────────────────────────────────────────┘
```

---

## 2. Cross-Platform Application Topology

AgentCommerce ships four distinct role applications with shared design tokens, authentication, API contracts, accessibility primitives, agent components, and observability:

| Application | Primary devices | Responsive desktop web | Installable mobile web | Native packaging target |
| --- | --- | --- | --- | --- |
| Customer | Android and iOS phones | Yes | Yes | Expo/React Native |
| Vendor | Desktop, tablet, Android, iOS | Yes | Yes | Expo/React Native where needed |
| Dasher | Android and iOS phones | Operational fallback only | Yes | Expo/React Native |
| Admin | Desktop and large tablet | Yes | Yes | Web-first; optional native shell |

Each application has separate navigation, authorization boundaries, notification channels, offline/degraded states, and role-specific agent tools. Shared packages must not create cross-role data access.

Phase 1 supports text plus push-to-talk speech input and optional text-to-speech responses where device/browser capability permits. Phase 2 adds low-latency, interruptible, bidirectional real-time voice. Every voice action has an equivalent visual confirmation and manual workflow.

Current responsive PWA routes are `/customer/`, `/vendor/`, `/dasher/`, and `/admin/`. They establish the role information architecture and REST integration shell; native store packaging remains a separate build/release task.

## 3. Global Design System & Aesthetic Foundations

All portals share a cohesive, premium design system designed for maximum legibility, high visual hierarchy, micro-animations, and instant visual feedback during AI agent execution.

### 3.1 Color Tokens (Dark & Light Mode HSL)

| Token | Dark Mode (Default) | Light Mode | Usage |
| --- | --- | --- | --- |
| `--bg-canvas` | `hsl(222, 47%, 7%)` | `hsl(210, 40%, 98%)` | Root application background |
| `--bg-surface` | `hsl(222, 40%, 11%)` | `hsl(0, 0%, 100%)` | Card containers, sidebar, topbar |
| `--bg-surface-elevated` | `hsl(222, 38%, 16%)` | `hsl(210, 30%, 95%)` | Dropdowns, modals, floating docks |
| `--primary-emerald` | `hsl(158, 64%, 52%)` | `hsl(158, 70%, 42%)` | Primary CTAs, active states, agent confirmation |
| `--agent-cyan` | `hsl(192, 95%, 55%)` | `hsl(192, 90%, 45%)` | Agent voice waveform, thinking glow, tool chips |
| `--accent-amber` | `hsl(38, 92%, 50%)` | `hsl(38, 92%, 48%)` | Busy mode, pending approvals, warnings |
| `--danger-rose` | `hsl(348, 83%, 60%)` | `hsl(348, 83%, 50%)` | Rejections, cancellations, emergency pause |
| `--text-primary` | `hsl(210, 40%, 98%)` | `hsl(222, 47%, 11%)` | Primary headers, data values |
| `--text-secondary` | `hsl(215, 20%, 65%)` | `hsl(215, 16%, 47%)` | Secondary metadata, labels, timestamps |
| `--border-subtle` | `hsl(222, 30%, 20%)` | `hsl(214, 32%, 91%)` | Dividers, card strokes, input borders |

### 3.2 Typography Hierarchy (Inter & Outfit)
- **Brand / Display Titles**: `Outfit`, 600/700 weight with tight tracking (`-0.02em`).
- **Data & Navigation**: `Inter`, 400/500/600 weight with high contrast tabular numbers.
- **Agent Monospace Logs**: `JetBrains Mono` for JSON payloads, tokens, coordinates, and latency.

---

## 4. Portal 1: Admin & Call Center Operations Console ("Mission Control")

### 4.1 Layout Architecture
The Admin Console is structured as an ultra-dense, real-time command center for call center operators and dispute mediators.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ TOP BAR: Global Status | Active Orders: 142 | Delayed: 3 | AI Safety: ALL NORMAL | Voice Co-Pilot │
├───────────────────┬──────────────────────────────────────────┬──────────────────────────────┤
│ 1. TRIAGE QUEUE   │ 2. 360° ORDER TRIAD DETAIL               │ 3. AGENT & DISPUTE RESOLVER  │
│                   │                                          │                              │
│ [High Priority]   │ Order: #ORD-9842 (Multi-Vendor: 2 Stops) │ 3-Way Live Transcript Feed   │
│ • #ORD-9842 (Late)│ Customer: Sarah J. (⭐ 4.9)              │ [Customer]: "Missing drink"  │
│ • #DISP-104 (Damg)│ ├─ Vendor 1: Mario's Pizza (Ready 12m)   │ [Driver]: "Handed 2 bags"    │
│ • #APP-401 (Price)│ └─ Vendor 2: Fresh Market (Ready 5m)     │ [Vendor]: "Drink was inside" │
│                   │ Driver: Carlos R. (🚗 On Route to V1)    │ ──────────────────────────── │
│ [Live Live Chat]  │ ──────────────────────────────────────── │ 🤖 Voice AI Agent Co-Pilot   │
│ • Customer Sarah  │ Live Spatial Map View & Route Progress   │ Suggested Action:            │
│ • Driver Carlos   │ Estimated Dropoff: 12:44 PM (3m delay)   │ [Issue $4.50 Drink Refund]   │
│ • Vendor Bella    │ Proof Photos: [Item Bag] [Dropoff Door]  │ [Re-dispatch Replacement]    │
└───────────────────┴──────────────────────────────────────────┴──────────────────────────────┘
```

### 4.2 Key Views & Features
1. **360° Order Triad Synchronizer**:
   - Simultaneously renders Customer profile, all Vendor sub-orders (with preparation clocks), and the assigned Driver's live GPS telemetry.
2. **One-Click Dispute & Refund Engine**:
   - Compares customer claim photos, driver pickup barcodes, and vendor packaging logs.
   - Calculates policy-allowed partial refund limits automatically with instant Stripe refund execution.
3. **Phase 2 Call Center AI Voice Co-Pilot**:
   - **Real-Time Telephony Transcription**: Listens to inbound operator phone calls in real time.
   - **Live Sentiment & Threat Detection**: Detects caller frustration and flags prompt-injection or fraudulent claims.
   - **Real-Time Knowledge Assist**: Automatically surfaces the exact order timeline, recipe ingredients, or driver location before the operator has to manually search.

---

## 5. Portal 2: Vendor Storefront, Menu Studio & Kitchen Tablet

### 5.1 Dual-Mode Catalog & Inventory Architecture
To handle both **Restaurants** and **Grocery/Convenience Retailers**, the portal provides two synchronized catalog modes:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ RESTAURANT MENU STUDIO                           │ GROCERY & RETAIL INVENTORY STUDIO        │
├──────────────────────────────────────────────────┼──────────────────────────────────────────┤
│ • Sections: Appetizers, Entrees, Desserts, Drinks│ • Departments: Fresh Produce, Dairy, Dry │
│ • Dish Modifier Groups:                          │ • Universal Barcode (UPC / SKU) Search   │
│    - Required (e.g. Choose 1 Protein)            │ • Inventory Units: Per Lb, Ounce, Pack   │
│    - Optional (e.g. Extra Avocado +$1.50)        │ • Real-time Quantity-on-Hand Tracking    │
│ • Visual Availability: 1-Click "86'd" Out-of-Stock│ • Low-Stock Alerts & Auto-Reorder Alerts │
│ • Prep Times: Base Prep (15m) + Busy Padding     │ • Bulk CSV Import / Export Staging Engine│
└──────────────────────────────────────────────────┴──────────────────────────────────────────┘
```

### 5.2 Kitchen Tablet View ("Live Expediter")
Designed for high-contrast touchscreens in busy kitchen environments:
- **Audio Order Alerts**: Distinct audible chimes for incoming sub-orders with auto-escalating volume.
- **1-Tap Actions**:
  - `Accept (+15m)` / `Accept (+25m)` / `Reject (Specify Reason)`.
  - `Mark Ready for Pickup` (Triggers immediate driver arrival notification).
- **1-Tap Kitchen Emergency Controls**:
  - `Busy Mode (+10m prep buffer)`: Dynamically adds 10 minutes to all future order quotes during rushes.
  - `Pause Store (30m / 1h / Indefinite)`: Temporarily pauses marketplace orders with auto-resume countdown timer.

### 5.3 Phase 2 Hands-Free Kitchen Voice Assistant
In hot and fast-paced kitchens where staff wear gloves and cannot touch screens:
- **Wake-Word / Push-to-Talk Voice Interface**:
  - *"Agent, 86 the Garlic Bread for the evening."* → Instantly marks item unavailable on the live customer catalog.
  - *"Accept new order with 20 minutes prep."* → Confirms sub-order #402.
  - *"How many burger orders are currently on the line?"* → Agent reads out live prep queue count.

---

## 6. Portal 3: Customer Marketplace & Multi-Vendor Discovery Experience

### 6.1 Multi-Vendor Shopping Architecture
- **Neighborhood Cart Drawer**: Allows customers to add items from multiple local vendors (e.g., Artisan Pizza + Craft Ice Cream + Corner Convenience) into **one single coordinated delivery checkout**.
- **Transparent Price Breakdown**:
  - Vendor In-Store Base Price: `$14.00`
  - Marketplace Platform Fee (`X%`): `$2.10`
  - Delivery Fee: `$3.99`
  - Estimated Taxes & Tip: `$4.50`
  - **Itemized Total Quote**: `$24.59` (Guaranteed price snapshot for 10 minutes).

### 6.2 Phase 1: Natural Language Conversational Assistant
- **Docked AI Chat Bar**:
  - *"Find me 2 gluten-free pepperoni pizzas and vegan gelato from stores within 3 miles."*
  - The AI agent queries live PostGIS availability, generates rich product comparison cards, and populates the multi-vendor cart with 1 click.

---

## 7. Phase 2: Real-Time Voice Agent Multi-Modal Transition

### 7.1 Real-Time Audio Architecture (WebRTC + Speech-to-Speech)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                            VOICE PIPELINE & VISUAL CANVAS SYNC                              │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ User Voice Input                                                                            │
│       │ (WebRTC Audio Stream / 24kHz Opus)                                                  │
│       ▼                                                                                     │
│ Voice Activity Detection (VAD) ──► Audio Barge-In Interrupt Handler                         │
│       │                                                                                     │
│       ▼                                                                                     │
│ Gemini 2.0 Realtime / Speech-to-Speech Agent Core                                           │
│       │                                                                                     │
│       ├─────────────────────────────────┬─────────────────────────────────┐                 │
│       ▼                                 ▼                                 ▼                 │
│ Low-Latency Audio Stream        Structured Tool Calls             Visual UI State Sync      │
│ (200ms TTFB Audio Output)       (Deterministic Commerce APIs)     (Highlighted Cards / Cart)│
│       │                                 │                                 │                 │
│       ▼                                 ▼                                 ▼                 │
│ Speaker / Headset               Order / Inventory Database        Interactive Web/App Canvas│
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Voice UI Components & Visual States
When the user activates Voice Mode, the UI transforms with a fluid, multi-modal interface:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ VOICE AGENT FLOATING ORB STATES                                                             │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 1. [ LISTENING ]   ► Cyan Pulsing Radial Glow (Expands with User Audio Frequency)           │
│ 2. [ THINKING ]    ► Emerald & Cyan Rotating Gradient Ring (Tool Invocation in progress)   │
│ 3. [ SPEAKING ]    ► Audio Frequency Waveform Bars with Live Word-by-Word Captioning        │
│ 4. [ BARGE-IN ]    ► Instant Snap to Listening when User speaks over the Agent              │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Synchronized Visual Canvas & Step-Up Voice Confirmation
- **Visual-Voice Pairing**: As the Voice Agent speaks (*"I found 3 great burger spots nearby: Bella Bistro, BurgerCraft, and Urban Grill"*), the main screen automatically scrolls and highlights the 3 respective vendor cards in sync with the audio.
- **Voice Step-Up Approval Security**:
  - Placing an order or committing financial transactions requires deterministic verbal confirmation:
  - *Agent: "Your total from Mario's Pizza and Sweet Scoops is $28.50. Would you like me to charge your card ending in 4242?"*
  - *Customer: "Yes, place the order."*
  - The voice agent validates the cryptographic approval token, authorizes the payment intent, and displays the live order tracking animation on screen.

---

## 8. Portal 4: Driver Hands-Free Voice Assistant

### 8.1 Eyes-on-the-Road Audio Navigation
Drivers navigating multi-pickup routes cannot look at phone screens while driving:
- **Audio Stop Guidance**: *"Next stop: Bella Italia. Turn left on Elm Street. Parking spot #4 is reserved for delivery drivers in the back alley."*
- **Voice Arrival & Pickup Confirmation**:
  - Driver: *"I've arrived at Bella Italia."* → App confirms geofence arrival.
  - Driver: *"Confirm pickup code 4-0-2."* → Marks Vendor Sub-Order 1 as `picked_up` and automatically recalculates the route to Vendor Sub-Order 2.
- **Masked Voice Contact**:
  - Driver: *"Call customer to notify gate code needed."* → Agent dials the customer via masked Twilio bridge without exposing raw phone numbers.

---

## 9. Implementation Roadmap (Phase 1 to Phase 2)

| Stage | Milestones | Primary Deliverables |
| --- | --- | --- |
| **Stage 1: Design System & Tokens** | Core Tokens, Typography, Dark/Light Themes | `index.css`, Base UI Component Library |
| **Stage 2: Admin Operations Console** | 360° Order Triad, Live Dispute Resolver, Safety Killswitch | Admin Portal Frontend + Mock Sockets |
| **Stage 3: Vendor Menu & Tablet Portal** | Restaurant Menu Builder, Grocery Inventory Grid, Kitchen Live Tablet | Vendor Portal + Tablet Touch UX |
| **Stage 4: Customer Conversational Web** | Natural Language Multi-Vendor Shopping, Cart Drawer, Checkout Quotes | Customer Web Portal + NL Chat Dock |
| **Stage 5: Phase 2 Real-Time Voice Agent** | WebRTC Audio Streaming, Voice Orb Waveform, Audio Barge-In, Step-Up Voice Approvals | Real-time Voice SDK Integration |

---

## 10. Approval Checklist

- [x] Multi-portal architecture (Admin, Vendor, Customer, Driver) accepted.
- [x] Dual-mode catalog architecture (Restaurant Menus + Grocery Inventories) defined.
- [x] Phase 1 Natural Language Conversational Dock and Inline Tool Cards specified.
- [x] Phase 2 Real-Time Voice Streaming, Waveforms, Audio Barge-In, and Voice Approvals specified.
- [x] Ready for Stage 1 & 2 UI Frontend implementation.
