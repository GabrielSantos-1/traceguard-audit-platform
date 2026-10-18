# TraceGuard Audit Platform

> Kit de Governança seguro, pronto para deploy — API Spring Boot com JWT, RBAC, rate limiting, auditoria e isolamento multi-tenant.

[![Backend CI](https://github.com/GabrielSantos-1/traceguard-audit-platform/actions/workflows/traceguard-backend-ci.yml/badge.svg?branch=master)](https://github.com/GabrielSantos-1/traceguard-audit-platform/actions/workflows/traceguard-backend-ci.yml)
[![Java](https://img.shields.io/badge/Java-21-%23ED8B00)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-%236DB33F)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-%23336791)](https://postgresql.org)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D)](https://redis.io)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## Sobre

TraceGuard é uma plataforma backend pronta para projetos que exigem **governança, auditoria e segurança** desde o primeiro commit. Ela fornece:

- **Autenticação** JWT com refresh token rotation e revogação
- **Autorização** RBAC com perfis admin e operador
- **Rate limiting** resiliente (Redis + fallback in-memory)
- **Auditoria** administrativa e de eventos com sanitização
- **Multi-tenancy** com isolação por API key
- **Correlation ID** em toda a cadeia de requests
- **Logging estruturado** sem vazamento de dados sensíveis
- **Docker Compose** pronto para dev, staging e produção
- **13 documentos de governança** reutilizáveis em `context/`

---

## Funcionalidades

| Funcionalidade | Status |
|---|---|
| JWT com refresh token rotation | ✅ |
| RBAC (admin / operator) | ✅ |
| Rate limiting Redis + in-memory | ✅ |
| Auditoria administrativa | ✅ |
| API key management multi-tenant | ✅ |
| Correlation ID + logging | ✅ |
| Segredos externalizados (fail-fast) | ✅ |
| Docker Compose (API + Postgres + Redis) | ✅ |
| CI/CD com dependency review | ✅ |
| OpenAPI / Swagger UI | ✅ |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 (Temurin) |
| Framework | Spring Boot 3.4.3 |
| Database | PostgreSQL 16 |
| Cache / Rate limit | Redis 7 |
| Migrations | Flyway |
| Auth | JWT (jjwt 0.12.6) |
| Container | Docker + Compose |
| API Docs | OpenAPI 3.1 + Swagger UI |
| CI | GitHub Actions |

---

## Pré-requisitos

- **Java** 21+ (Temurin recomendado)
- **Docker** + **Docker Compose** plugin
- **Maven** Wrapper (incluído — `./mvnw`)

---

## Quickstart

### Script automático

```bash
chmod +x scripts/setup.sh && ./scripts/setup.sh
```

### Manual (3 comandos)

```bash
cp backend/infra/.env.example backend/infra/.env

cd backend/traceguard-api && ./mvnw -q -DskipTests package && cd ../..

docker compose -f backend/infra/docker-compose.yml --project-name traceguard up -d --build
```

### Swagger UI

Acesse `http://localhost:8080/swagger-ui.html` para explorar e testar todos os endpoints interativamente.

### Verificar saúde

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### Primeiro login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@traceguard.local","password":"admin123"}'
```

---

## Variáveis de ambiente

| Variável | Obrigatório | Default (dev) | Descrição |
|---|---|---|---|
| `DB_URL` | sim | `jdbc:postgresql://db:5432/traceguard` | URL do PostgreSQL |
| `DB_USERNAME` | sim | `traceguard` | Usuário do banco |
| `DB_PASSWORD` | **sim** | — | Senha do banco |
| `REDIS_URL` | sim | `redis://redis:6379` | URL do Redis |
| `JWT_SECRET` | **sim** | — | Chave JWT (mín. 32 caracteres) |
| `JWT_ISSUER` | não | `traceguard-api` | Issuer do JWT |
| `JWT_AUDIENCE` | não | `traceguard-admin-api` | Audience do JWT |
| `RATE_LIMIT_REDIS_ENABLED` | não | `true` | Habilita rate limit via Redis |
| `SEED_ADMIN_EMAIL` | não (dev) | `admin@traceguard.local` | Email do admin inicial (seed) |
| `SEED_ADMIN_PASSWORD` | não (dev) | `admin123` | Senha do admin inicial (seed) |
| `SPRING_PROFILES_ACTIVE` | não | `dev` | Perfil ativo |
| `SERVER_PORT` | não | `8080` | Porta HTTP |

> `DB_PASSWORD` e `JWT_SECRET` **não têm default seguro** — o sistema falha na inicialização se ausentes em staging/prod.

---

## Endpoints principais

### Auth
| Método | Path | Descrição |
|---|---|---|
| POST | `/api/v1/auth/login` | Login (email + senha) → JWT |
| POST | `/api/v1/auth/refresh` | Refresh token |
| POST | `/api/v1/auth/logout` | Logout (revoga refresh token) |

### Admin (requer papel `ADMIN`)
| Método | Path | Descrição |
|---|---|---|
| GET | `/api/v1/admin/tenants` | Listar tenants |
| POST | `/api/v1/admin/tenants` | Criar tenant |
| PUT | `/api/v1/admin/tenants/{id}` | Atualizar tenant |
| GET | `/api/v1/admin/api-keys` | Listar API keys |
| POST | `/api/v1/admin/api-keys` | Criar API key |
| DELETE | `/api/v1/admin/api-keys/{id}` | Revogar API key |
| GET | `/api/v1/admin/audit` | Consultar auditoria admin |

### Audit Events (requer API key)
| Método | Path | Descrição |
|---|---|---|
| POST | `/api/v1/audit/events` | Ingerir evento de auditoria |

---

## Exemplos de uso

> Assumindo seed padrão (`admin@traceguard.local` / `admin123`) e ambiente rodando em `localhost:8080`.

### 1. Login (obter JWT)

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@traceguard.local","password":"admin123"}' | jq .
```

Resposta:
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "a1b2c3d4-...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshExpiresIn": 604800,
    "user": {
      "id": "uuid-do-admin",
      "tenantId": "uuid-do-tenant",
      "role": "SUPER_ADMIN"
    }
  }
}
```

### 2. Criar tenant (requer admin)

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/tenants \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer SEU_JWT_AQUI' \
  -d '{"name":"Acme Corp","slug":"acme-corp"}' | jq .
```

### 3. Criar API key (requer admin)

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/api-keys \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer SEU_JWT_AQUI' \
  -d '{
    "tenantId": "uuid-do-tenant",
    "label": "minha-app",
    "scope": "INGESTION"
  }' | jq .
```

Guarde o `rawApiKey` retornado — ele só é exibido uma vez.

### 4. Ingerir evento de auditoria (requer API key)

```bash
curl -s -X POST http://localhost:8080/api/v1/audit/events \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: SUA_API_KEY_AQUI' \
  -d '{
    "action": "DOCUMENT_ACCESSED",
    "resourceType": "DOCUMENT",
    "resourceId": "doc-123",
    "actorId": "user-456",
    "metadata": {
      "ip": "192.168.1.10",
      "documentName": "contrato.pdf"
    }
  }' | jq .
```

### 5. Consultar auditoria admin

```bash
curl -s http://localhost:8080/api/v1/admin/audit \
  -H 'Authorization: Bearer SEU_JWT_AQUI' | jq .
```

---

## Estrutura do projeto

```
├── backend/
│   ├── infra/
│   │   ├── docker-compose.yml
│   │   └── .env.example
│   └── traceguard-api/
│       ├── Dockerfile
│       ├── pom.xml
│       ├── mvnw
│       └── src/
│           ├── main/java/com/traceguard/
│           │   ├── adminaudit/     → auditoria administrativa
│           │   ├── apikey/         → gestão de API keys
│           │   ├── audit/          → eventos de auditoria
│           │   ├── auth/           → autenticação JWT + refresh
│           │   ├── common/         → error handling, correlation ID, sanitização
│           │   ├── ratelimit/      → rate limiting (Redis + in-memory)
│           │   ├── security/       → security config, API key filter
│           │   └── tenant/         → multi-tenancy
│           └── test/
├── context/
│   ├── ARCHITECTURE.md           → arquitetura e camadas
│   ├── SECURITY_RULES.md         → regras de segurança (prioridade máxima)
│   ├── API_CONTRACTS.md          → contratos de API
│   ├── DECISIONS.md              → registro de decisões (ADR)
│   └── ...                       → +9 documentos de governança
└── LICENSE
```

---

## Documentação

O projeto inclui um **kit completo de governança** em `context/` — 13 documentos que cobrem arquitetura, segurança, padrões de código, contratos de API e processo de desenvolvimento. Comece por `context/CONTEXT_MAP.md`.

Para adaptar este template ao seu projeto, veja o [Guia de Customização](CUSTOMIZATION.md).

Documentação operacional adicional em `backend/traceguard-api/docs/`:
- `OPERATIONS_RUNBOOK.md` — procedimentos de deploy e rollback
- `SECRETS_POLICY.md` — política de gerenciamento de segredos
- `PRODUCTION_READINESS_CHECKLIST.md` — checklist para produção

---

## Licença

Distribuído sob licença MIT. Veja [LICENSE](LICENSE) para mais informações.
