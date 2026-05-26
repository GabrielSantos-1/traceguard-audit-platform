# TraceGuard Audit Platform — Gumroad Product Page

## Title
TraceGuard Audit Platform — Spring Boot Governance & Audit Kit

## Tagline
Production-ready audit platform with JWT auth, RBAC, multi-tenancy, API keys, and rate limiting. Bootstrap your next enterprise-grade backend in minutes.

## Price: $19 USD

---

## Description

### What is TraceGuard?

TraceGuard is a **production-ready audit and governance platform** built with Spring Boot 3.4 and Java 21. It's the backend foundation for any SaaS application that needs:

- ✅ **JWT authentication** with refresh token rotation
- ✅ **RBAC** with 4 roles (SUPER_ADMIN, TENANT_ADMIN, OPERATOR, AUDITOR)
- ✅ **Multi-tenancy** with full data isolation
- ✅ **API key authentication** for machine-to-machine integration
- ✅ **Dual audit trails** (admin actions + business events)
- ✅ **Rate limiting** per endpoint (bucket-based, Redis + in-memory)
- ✅ **Correlation ID** tracing end-to-end
- ✅ **Swagger/OpenAPI** documentation
- ✅ **PostgreSQL + Redis** (with H2 in-memory for local dev without Docker)

### Who is this for?

- **SaaS founders** who need a secure backend foundation
- **Backend developers** who want a reference architecture for Spring Boot
- **Tech leads** who need an auditable, governance-compliant system
- **Consultants** building multi-tenant applications

### What's included?

```
traceguard-audit-platform-v1.0.0/
├── backend/
│   └── traceguard-api/          # Complete Spring Boot application
│       ├── src/main/java/        # 80 Java source files
│       └── src/main/resources/   # Configs, migrations, frontend
├── context/                      # 13 governance documents
├── scripts/                      # Setup & automation
├── README.md                     # Full documentation
├── CUSTOMIZATION.md              # 285-line rebranding guide
└── LICENSE (MIT)
```

### Key Features

| Feature | Description |
|---------|-------------|
| **JWT Auth** | Login, refresh rotation, logout — full session lifecycle |
| **RBAC** | 4 roles with per-endpoint authorization |
| **Multi-tenant** | Complete data isolation by tenantId |
| **API Keys** | Scoped keys (AUDIT_READ / AUDIT_WRITE) for M2M |
| **Audit Trails** | Admin events + business events with full search |
| **Rate Limiting** | 6 configurable buckets (login, refresh, ingest, query, admin read/write) |
| **Correlation IDs** | Every request traceable from edge to database |
| **OpenAPI/Swagger** | Auto-generated docs with JWT + API key schemes |
| **Flyway Migrations** | 10 versioned migrations. Incremental, safe, reversible |
| **Docker Compose** | Full stack (API + PostgreSQL + Redis) in 2 commands |

### Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐
│  JWT Auth   │ ──→ │  Controller  │ ──→ │   Service  │ ──→ DB
│  API Keys   │     │  (RBAC)      │     │  (Isolated)│
└─────────────┘     └──────────────┘     └────────────┘
     │                     │                     │
     ▼                     ▼                     ▼
Rate Limit Filter    Correlation ID         Tenant Filter
```

### Quickstart

```bash
# Prerequisites: Java 21, Docker (optional)
unzip traceguard-audit-platform-v1.0.0.zip
cd backend/traceguard-api

# Option A: With Docker (PostgreSQL + Redis)
docker compose -f ../infra/docker-compose.yml up -d
./mvnw spring-boot:run -Dspring.profiles.active=dev

# Option B: Without Docker (H2 in-memory)
./mvnw spring-boot:run -Dspring.profiles.active=dev,h2

# Seed login (dev only):
# Email:    admin@traceguard.local
# Password: admin123
```

### API Endpoints

```
POST   /api/v1/auth/login         → JWT access + refresh tokens
POST   /api/v1/auth/refresh       → Rotate refresh token
POST   /api/v1/auth/logout        → Revoke refresh token
POST   /api/v1/admin/tenants      → Create tenant (SUPER_ADMIN)
GET    /api/v1/admin/tenants      → List tenants (SUPER_ADMIN, AUDITOR)
PATCH  /api/v1/admin/tenants/{id} → Update tenant (SUPER_ADMIN)
POST   /api/v1/admin/api-keys     → Create API key
GET    /api/v1/admin/api-keys     → List API keys
PATCH  /api/v1/admin/api-keys/{id}/revoke → Revoke API key
GET    /api/v1/admin/audit-events → Query admin audit trail
POST   /api/v1/audit/events       → Ingest audit event (API Key)
GET    /api/v1/audit/events       → Query audit events (API Key)
```

### Customization

Includes a **285-line CUSTOMIZATION.md** guide covering:
1. Rename Java packages (with bash commands)
2. Rename project directories
3. Update Docker configurations
4. Configure Spring Boot profiles
5. Set environment variables for staging/production
6. Update CI/CD workflows
7. Rebrand documentation

---

## Tags (for Gumroad Discover)

`spring-boot`, `java-21`, `audit`, `governance`, `saas-template`, `multi-tenant`, `rbac`, `jwt-authentication`, `api-keys`, `rate-limiting`, `backend-template`, `spring-security`, `rest-api`, `microservices`, `devops`, `flyway`, `postgresql`, `redis`, `enterprise-java`, `open-source`

---

## Pricing Strategy

| Tier | Price | What's included |
|------|-------|-----------------|
| **Basic** | $19 | Complete source code + documentation + customization guide |
| **Bundle** | $39 | (Future) TraceGuard + Verde Vale Connect template |

---

## License

MIT License — free for commercial use, modify, and distribute.
