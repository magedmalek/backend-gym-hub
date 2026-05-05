# GymHub Backend — Development Guide

## Overview

Spring Boot backend for **GymHub** — a full gym management system.

Stack: Spring Boot 3.5.4 · MySQL · Docker · JWT · Cloudinary · Infobip

---

## Business Modules

| Epic | Module | Key entities |
|------|--------|-------------|
| 1 | Identity & Auth | `User`, roles, JWT context |
| 2 | Gym (Service Provider) | `Gym`, `GymSettings` |
| 3 | Employee Management | `Employee`, `EmployeePermission` |
| 4 | Services & Packages & Subscriptions | `GymService`, `GymPackage`, `Subscription`, `Payment` |
| 5 | Customers, Attendance, Invitations, Extra Services | `Customer`, `Attendance`, `Invitation`, `ExtraServiceTransaction`, `ServiceUsage` |

---

## Prerequisites

- Docker & Docker Compose installed
- Java 17 (only needed if running outside Docker)

---

## First Time Setup

### 1. Clone the repo

```bash
git clone https://github.com/your-org/gymhub.git
cd gymhub
```

### 2. Create your `.env` file

Copy `.env.example` to `.env` — never commit `.env`.

```bash
cp .env.example .env
```

Then edit `.env` with your values:

```env
# Database
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=gymhub_db
MYSQL_USER=gymhub
MYSQL_PASSWORD=gymhub

# App datasource
DATASOURCE_URL=jdbc:mysql://mysql:3306/gymhub_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATASOURCE_USERNAME=gymhub
DATASOURCE_PASSWORD=gymhub

# JWT — any long Base64 random string works locally
JWT_SECRET=local-dev-secret-replace-in-prod

# Mail — fake values are fine unless testing email
MAIL_USERNAME=fake@fake.com
MAIL_PASSWORD=fake

# Cloudinary — fake values are fine unless testing image upload
CLOUDINARY_CLOUD_NAME=fake
CLOUDINARY_API_KEY=fake
CLOUDINARY_API_SECRET=fake

# Infobip — fake values are fine unless testing SMS
INFOBIP_API_KEY=fake
INFOBIP_BASE_URL=https://fake.api.infobip.com

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080

# Server port
SERVER_PORT=8085
```

> The app starts fine with fake Cloudinary / Infobip / Mail values — those features simply fail when triggered.

---

## Running Locally

### Build and start all containers

```bash
docker compose up --build
```

### Rebuild only the app after code changes

```bash
docker compose up --build app
```

### Stop all containers

```bash
docker compose down
```

### Wipe database and start fresh

```bash
docker compose down -v
```

---

## Verifying the App is Running

```bash
# Check running containers
docker ps

# Follow app logs
docker compose logs app -f

# Swagger UI
open http://localhost:8085/swagger-ui.html

# OpenAPI JSON
curl http://localhost:8085/v3/api-docs
```

---

## API Overview

All endpoints are prefixed with `/api/v1`.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register a new unified user |
| POST | `/auth/login` | Login — returns access + refresh JWT |
| POST | `/gyms` | Create a gym (caller becomes owner) |
| GET  | `/gyms/my` | List the caller's gyms |
| PUT  | `/gyms/{id}/settings` | Update activation policy, entry methods, etc. |
| POST | `/gyms/{id}/employees` | Add an employee |
| PUT  | `/gyms/{id}/employees/{eid}/permissions` | Update permissions |
| POST | `/gyms/{id}/services` | Create a service/class (free name) |
| POST | `/gyms/{id}/packages` | Create a subscription package |
| POST | `/gyms/{id}/customers` | Register a customer |
| GET  | `/gyms/{id}/customers/search?q=` | Search by name / email / member code |
| GET  | `/gyms/{id}/customers/by-code/{code}` | Barcode / QR lookup |
| POST | `/gyms/{id}/subscriptions/sell` | Sell a subscription |
| POST | `/gyms/{id}/subscriptions/{sid}/payments` | Record a payment |
| POST | `/gyms/{id}/subscriptions/{sid}/activate` | Activate (with optional deferred start date) |
| POST | `/gyms/{id}/attendance` | Record member / guest entry |
| POST | `/gyms/{id}/invitations` | Use a guest invitation slot |
| POST | `/gyms/{id}/extra-services/sell` | Sell an add-on service (independent transaction) |
| POST | `/gyms/{id}/extra-services/usage` | Record use of a bundled service (no charge) |

Full interactive docs: `http://localhost:8085/swagger-ui.html`

---

## CI/CD Pipeline

### Workflow files

```
.github/workflows/
├── build.yml   ← triggers on push to main; builds & pushes Docker image to GHCR
└── deploy.yml  ← triggers when build succeeds; deploys to EC2 server
```

### Flow

**Push to main**

`build.yml`:
- Build Docker image
- Tag with short Git SHA (e.g. `a1b2c3d`)
- Push to `ghcr.io/your-org/gymhub:a1b2c3d`

`deploy.yml` (triggered automatically on build success):
- Write `.env` on EC2 from `ENV_FILE` GitHub secret
- `docker login` to ghcr.io
- `docker compose pull app`
- If MySQL running → restart app only
- If first deploy → start all services
- `docker image prune`

**Manual deploy** (redeploy without new build):
- Rollback: specify a previous SHA tag
- Secret update: apply new `ENV_FILE` secret without pushing code

---

## Secrets Management

All production secrets live in GitHub Secrets as `ENV_FILE`.

| File | Committed? | Purpose |
|------|-----------|---------|
| `.env` | ❌ Never | All local secrets |
| `.env.example` | ✅ Yes | Template (no real values) |
| `application-local.yml` | ✅ Yes | Non-secret local Spring config |
| `application-prod.yml` | ✅ Yes | Non-secret prod Spring config |

---

## Troubleshooting

**App container keeps restarting**
```bash
docker compose logs app --tail=100
```

**MySQL connection refused**
The app waits for MySQL to be healthy. If MySQL is slow, increase `start_period` in the healthcheck.
```bash
docker compose logs mysql
```

**Public Key Retrieval error**
Make sure `DATASOURCE_URL` contains `allowPublicKeyRetrieval=true`.

**JWT errors at startup**
The `JWT_SECRET` must be a valid Base64-encoded string of sufficient length (≥ 32 bytes decoded).

**Port already in use**
```bash
# Windows
netstat -ano | findstr :8085

# Linux / Mac
lsof -i :8085
```
