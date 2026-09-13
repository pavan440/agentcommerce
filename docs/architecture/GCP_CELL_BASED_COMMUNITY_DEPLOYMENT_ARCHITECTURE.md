# GCP Community-Cellular Monolith Deployment Architecture

**Version:** 1.0  
**Status:** Approved Architecture  
**Cloud Provider:** Google Cloud Platform (GCP)  
**Deployment Pattern:** Cell-Based / Community-Partitioned Modular Monolith  

---

## 1. Executive Summary

This architecture establishes a **Community-Cellular Modular Monolith** deployed on **Google Cloud Platform (GCP)**. 

While the codebase is engineered as a unified, high-velocity **Modular Monolith** (Java 21 Spring Boot + PostgreSQL PostGIS + Python Agent), deployment is partitioned by **Hyperlocal Community Cells** (e.g. `cell-vancouver-downtown`, `cell-toronto-west`, `cell-austin-south`).

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                       GCP COMMUNITY-CELLULAR ARCHITECTURE                                   │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ USER / VENDOR / DRIVER TRAFFIC                                                              │
│       │ (Geolocated DNS & Google Cloud Armor / Cloud CDN)                                   │
│       ▼                                                                                     │
│ CLOUD LOAD BALANCER (Path / Subdomain / Geohash Community Router)                          │
│       │                                                                                     │
│       ├─────────────────────────────────┬─────────────────────────────────┐                 │
│       ▼                                 ▼                                 ▼                 │
│ [ COMMUNITY CELL 1 ]            [ COMMUNITY CELL 2 ]            [ COMMUNITY CELL N ]        │
│ (e.g. Downtown Arts Dist)       (e.g. Westside Village)         (e.g. University Quad)      │
│ ┌─────────────────────────────┐ ┌─────────────────────────────┐ ┌─────────────────────────┐ │
│ │ Cloud Run: `domain-api`     │ │ Cloud Run: `domain-api`     │ │ Cloud Run: `domain-api` │ │
│ │ (Spring Boot Monolith)      │ │ (Spring Boot Monolith)      │ │ (Spring Boot Monolith)  │ │
│ │ ├─ In-Store POS Webhooks    │ │ ├─ In-Store POS Webhooks    │ │ ├─ In-Store POS Webhooks│ │
│ │ ├─ Flash Promo Engine       │ │ ├─ Flash Promo Engine       │ │ ├─ Flash Promo Engine   │ │
│ │ └─ Multi-Vendor Orders      │ │ └─ Multi-Vendor Orders      │ │ └─ Multi-Vendor Orders  │ │
│ ├─────────────────────────────┤ ├─────────────────────────────┤ ├─────────────────────────┤ │
│ │ Cloud Run: `agent-service`  │ │ Cloud Run: `agent-service`  │ │ Cloud Run: `agent-service`│
│ │ (Python Gemini Realtime)    │ │ (Python Gemini Realtime)    │ │ (Python Gemini Realtime)│ │
│ └─────────────────────────────┘ └─────────────────────────────┘ └─────────────────────────┘ │
│       │                                 │                                 │                 │
│       ▼                                 ▼                                 ▼                 │
│ ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ MANAGED POSTGRESQL (Cloud SQL / AlloyDB with PostGIS & pgvector)                        │ │
│ │ ├─ Tenant-isolated schemas by `community_zone_id`                                       │ │
│ │ └─ Centralized Merchant Payout & Global Identity Cluster                                │ │
│ └─────────────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Why Community-Cellular on GCP Cloud Run?

### 1. Zero Idle Cost & Scale-to-Zero ($0 at 4 AM)
Each community instance runs as a containerized **GCP Cloud Run Service**.
- If a quiet suburban neighborhood has zero orders between 1:00 AM and 6:00 AM, that community instance **scales down to 0 instances**—incurring **$0 in compute costs**.
- When orders surge in a bustling downtown district on Friday night, that specific cell auto-scales instantly to 100+ concurrent instances without affecting other communities.

### 2. Zero Blast Radius (Isolation)
- If a massive flash promotion in Community A creates heavy traffic, Community B’s instance is completely isolated and runs with full performance.

### 3. Hyper-Local Latency (< 10ms Response Time)
- Local store POS webhooks, driver GPS pings, and customer orders terminate in the closest Google Cloud edge point of presence (PoP).

---

## 3. GCP Managed Services Mapping

| Layer | GCP Service | Configuration / Notes |
| --- | --- | --- |
| **Edge & Routing** | Cloud Load Balancing + Cloud Armor | Routes by `X-Community-Zone` header or subdomain (`downtown.agentcommerce.com`). DDoS & WAF protection. |
| **API Backend** | Cloud Run (Serverless Containers) | Spring Boot modular monolith (`domain-api`). 1 vCPU, 512MB RAM minimum, auto-scales 0 to N. |
| **AI Agent Service** | Cloud Run + Vertex AI | Python FastAPI service with direct low-latency VPC connection to **Gemini 2.0 Flash**. |
| **Primary Database** | Cloud SQL for PostgreSQL 16 | PostGIS spatial extensions + `pgvector` for agent memory. Automatic backups and read replicas. |
| **Caching & Pub/Sub** | Memorystore for Redis + Cloud Pub/Sub | Cross-community event bus and live driver location presence. |
| **Object Storage** | Google Cloud Storage (GCS) | Store menus, CSV imports, customer claim photos, driver drop-off proof. |
| **Secrets & Keys** | Secret Manager | Stripe API keys, OAuth tokens, webhook signing secrets. |

---

## 4. Community Routing & Data Partitioning

### 4.1 Ingress Routing
Incoming HTTP/WebSocket traffic is routed to the corresponding community instance via:
1. **Subdomain Routing**: `https://{community-slug}.agentcommerce.app/v1/...`
2. **Header Routing**: `X-Community-Zone: {zone_uuid}`
3. **Geospatial Fallback**: PostGIS point lookup from customer lat/lon.

### 4.2 Database Multi-Tenancy Strategy
- **Shared Cluster with Partitioned Rows**: All tables include `community_zone_id` with composite indexes (`community_zone_id, created_at`).
- **Global Identity & Billing**: User authentication (`users`, `user_identities`), global driver profiles, and Stripe Merchant accounts operate centrally, while orders, menus, flash promotions, and inventory stream inside the local community cell.

---

## 5. Deployment & CI/CD Pipeline (Google Cloud Build)

```text
Git Commit 
  ──► Cloud Build 
  ──► Build Jib/Docker Container 
  ──► Artifact Registry (`us-docker.pkg.dev/agentcommerce/domain-api`)
  ──► Terraform / gcloud deploy to Cloud Run Cells:
      ├── `deploy cell-downtown`
      ├── `deploy cell-westside`
      └── `deploy cell-suburbs`
```

---

## 6. Approval Checklist

- [x] GCP selected as primary cloud infrastructure provider.
- [x] Community-cellular modular monolith deployment pattern approved.
- [x] Cloud Run scale-to-zero cost optimization approved.
- [x] Cloud SQL PostgreSQL with PostGIS and pgvector selected.
- [x] Ready for Spring Boot REST API implementation.
