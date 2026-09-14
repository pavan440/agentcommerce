# Marketplace Portal

## Scope

The UI package contains four distinct dependency-free responsive progressive web applications plus the original REST validation portal, all served by Nginx. They exercise implemented APIs without changing production authentication behavior.

Open `http://localhost:3000` after starting Docker Compose. The Domain API remains available at `http://localhost:8080`.

Role application routes:

- Customer: `http://localhost:3000/customer/`
- Vendor: `http://localhost:3000/vendor/`
- Dasher: `http://localhost:3000/dasher/`
- Admin: `http://localhost:3000/admin/`
- REST validation portal: `http://localhost:3000/`

The role applications share a responsive design system and service worker. They run in Android, iOS, tablet, and desktop browsers and can be installed as PWAs. This does not yet represent App Store or Play Store native packaging; the target native architecture is Expo/React Native with shared API and design-system packages.

## Authentication

Select **API session** and provide:

- the Domain API base URL;
- a real bearer JWT from the configured OIDC development tenant.

The bearer token is stored only in browser `sessionStorage` and is cleared when the tab session ends. The portal does not provide header-based authentication, role escalation, or an authentication bypass.

The JWT must include the immutable `iss` and `sub` claims. Vendor actions work for any provisioned customer because vendor creation establishes the owner membership. Operator verification requires an account that already has the local `OPERATOR` role.

## Workspaces

### Customer

- Load and update profile preferences with optimistic locking.
- Add, list, and delete delivery addresses.
- Grant or revoke location-tracking and agent-personalization consent.

### Vendor

- Create a draft vendor and owner membership.
- Load the current user's vendor portfolio.
- Create a physical vendor location.
- Load and update location commerce settings.

The portal carries newly created vendor and location identifiers into the next form automatically.

### Dasher

- Start driver onboarding or update vehicle details.
- View verification and availability status.
- Add and delete PostGIS operating zones.
- Go online or offline when backend eligibility rules are satisfied.

### Operator

- Submit versioned dasher verification decisions.
- Requires an authenticated local `OPERATOR` role.

### REST Lab

The REST Lab sends arbitrary `GET`, `POST`, `PATCH`, and `DELETE` requests. Presets cover health, current profile, vendor portfolio, vendor creation, and dasher profile requests. It displays status, latency, and formatted JSON responses.

## Vendor Verification Walkthrough

1. Run `docker compose up --build`.
2. Open `http://localhost:3000` and configure **API session**.
3. Confirm the Overview identity card shows the authenticated customer.
4. Open **Vendor** and select **Create draft vendor**.
5. Select the created vendor and create a draft location.
6. Load commerce settings, enable **Accepting orders**, and save.
7. Open **REST Lab**, choose **GET my vendors**, and send the request.
8. Confirm API status codes and response payloads in the response pane.

## Validation

Run static checks:

```powershell
node --check portal/app.js
docker compose config --quiet
git diff --check
```

Build and smoke-test the portal image:

```powershell
docker compose build portal
docker run --rm agentcommerce-portal:latest nginx -t
```