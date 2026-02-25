# TraceGuard

Multi-tenant Audit & Activity Tracking Platform built for secure event tracking in distributed systems.

---

## Overview

TraceGuard is a stateless, multi-tenant backend designed to record and query audit events with strong tenant isolation and API key–based authentication.

It was built with a security-first mindset and production-grade architecture principles.

---

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- PostgreSQL 16
- Flyway (database migrations)
- Docker / Docker Compose
- JUnit 5 + Mockito

---

## Core Concepts

### Multi-Tenancy
- Each request belongs to a tenant.
- Tenant is resolved exclusively via API Key.
- No tenant_id is ever accepted via body or query params.
- Thread-safe `TenantContext` per request.

### Authentication
- API Key via `X-API-Key` header
- Keys are stored hashed (SHA-256)
- Constant-time comparison to prevent timing attacks
- Keys can be enabled/disabled
- Optional expiration support

### Authorization
Scope-based authorization per API key:
- `AUDIT_READ`
- `AUDIT_WRITE`

Each endpoint validates required scope before execution.

---

## Architecture


Controller → Service → Repository


- Stateless API
- DTO separated from Entity
- Pagination with `PageRequest`
- No entity exposure
- Explicit `ResponseEntity` returns

---

## Project Structure


backend/
├── traceguard-api/
│ ├── audit/
│ ├── security/
│ ├── common/
│ └── db/migration/
└── infra/
└── docker-compose.yml


---

## Security Design Decisions

- Never trust tenant input from request body
- No token logging
- Constant-time hash comparison
- Explicit scope validation
- Context cleared defensively in filter
- Test coverage for security filter behavior

---

## How to Run

### 1️⃣ Clone

```bash
git clone <repo-url>
cd traceguard
2️⃣ Configure Environment

Create .env file inside:

backend/infra/

Example:

DB_PASSWORD=strongpassword
3️⃣ Start PostgreSQL
docker compose -f backend/infra/docker-compose.yml up -d
4️⃣ Run Application
cd backend/traceguard-api
mvn spring-boot:run
Running Tests
mvn test
Example Request
POST /api/v1/audit/events
X-API-Key: your-api-key
Content-Type: application/json
Roadmap

JWT authentication (optional alternative to API key)

OpenAPI documentation

Advanced filtering (Specification)

Rate limiting per tenant

Observability improvements (metrics per tenant)