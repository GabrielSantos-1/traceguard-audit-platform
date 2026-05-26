# Guia do Template TraceGuard Audit Platform

**Versão 1.0.0 — Maio 2026**

---

## 1. Visão Geral

O TraceGuard Audit Platform é um **kit de governança e auditoria** pronto para produção, construído com Spring Boot 3.4 e Java 21. Ele fornece a base backend para qualquer aplicação SaaS que exija:

- Autenticação segura com JWT
- Controle de acesso baseado em papéis (RBAC)
- Isolamento multi-tenant
- API Keys para integração machine-to-machine
- Trilhas de auditoria completas
- Rate limiting contra abuso

### O que você está comprando

Você está adquirindo um **template de código-fonte completo** que pode usar como ponto de partida para seu próprio projeto. Todo o código é aberto (MIT) — você pode modificar, distribuir e usar comercialmente sem restrições.

---

## 2. Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                     HTTP Request                         │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Filter Chain (SecurityConfig)               │
│  CorrelationLog → JwtAuth → ApiKey → RateLimit → AuthZ  │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    REST Controller                       │
│              @RequestMapping("/api/v1/...")              │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│                      Service Layer                       │
│            Regras de negócio + TenantContext             │
└────────────┬──────────────┬──────────────┬──────────────┘
             ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   JPA        │ │   Flyway     │ │   Redis      │
│  Repository  │ │  Migration   │ │  Rate Limit  │
└──────────────┘ └──────────────┘ └──────────────┘
```

### Camadas

| Camada | Pacote | Responsabilidade |
|--------|--------|-----------------|
| Controller | `*.api.*Controller` | Validação de entrada, envelope de resposta |
| Service | `*.api.*Service` | Lógica de negócio, orquestração |
| Repository | `*.api.*Repository` | Acesso a dados (Spring Data JPA) |
| Entity | `*.api.*Entity` | Modelo de domínio JPA |
| DTO/Record | `*.api.dto.*` | Contratos de entrada/saída |

### Pacotes

| Pacote | Função |
|--------|--------|
| `auth/` | Autenticação JWT, refresh tokens, login/logout |
| `tenant/` | Gerenciamento multi-tenant |
| `apikey/` | Criação e validação de API keys |
| `audit/` | Eventos de auditoria de negócio |
| `adminaudit/` | Trilha de auditoria administrativa |
| `security/` | Configuração de segurança, filtros, context |
| `ratelimit/` | Rate limiting bucket-based |
| `common/` | Transversais: correlation ID, API envelope, sanitização |

---

## 3. Modelo de Segurança

### Fluxo de Autenticação

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│  Login   │ ──JWT──→│  API     │ ──Rota──→│  Novo    │
│ (email/  │         │  Admin   │  Refresh  │  Token   │
│  senha)  │         │  Actions │          │  Par     │
└──────────┘         └──────────┘         └──────────┘
                           │
                    ┌──────┴──────┐
                    ▼             ▼
            ┌────────────┐ ┌────────────┐
            │ SUPER_ADMIN│ │ TENANT_    │
            │ AUDITOR    │ │ ADMIN      │
            │            │ │ OPERATOR   │
            └────────────┘ └────────────┘

┌──────────┐         ┌──────────┐
│ API Key  │ ───────→│  Audit   │
│ (M2M)    │         │  Events  │
│ Scoped   │         │  (CRUD)  │
└──────────┘         └──────────┘
```

### Papéis (RBAC)

| Papel | Acesso |
|-------|--------|
| SUPER_ADMIN | Tudo — gestão de tenants, API keys, auditoria |
| TENANT_ADMIN | API keys do próprio tenant, auditoria |
| OPERATOR | Criar API keys do próprio tenant |
| AUDITOR | Consultar tenants e auditoria (somente leitura) |

### API Key Scopes

| Scope | Permissão |
|-------|-----------|
| AUDIT_READ | Consultar eventos de auditoria |
| AUDIT_WRITE | Ingerir eventos de auditoria |

---

## 4. Customização (Renomear o Template)

### 4.1. Pacote Java

```bash
# Renomear diretórios
find backend/traceguard-api/src -type d -name "traceguard" -exec bash -c \
  'mkdir -p ${0//traceguard/seuprojeto} && mv "$0"/* ${0//traceguard/seuprojeto}/' {} \;

# Renomear referências em arquivos .java
find backend/traceguard-api/src -name "*.java" -exec sed -i 's/com\.traceguard/com.seuprojeto/g' {} +
```

### 4.2. Projeto

```bash
mv backend/traceguard-api backend/seuprojeto-api
```

### 4.3. Docker

Edite `backend/infra/docker-compose.yml`:
- Container names
- Service names  
- Image tags

### 4.4. Variáveis de Ambiente

Copie `backend/infra/.env.example` para `.env` e configure:
```bash
DB_PASSWORD=sua_senha_segura
JWT_SECRET=sua_chave_com_pelo_menos_32_caracteres
```

---

## 5. Deploy em Produção

### Pré-requisitos

- PostgreSQL 16
- Redis 7
- Java 21 JRE
- Acesso a um vault de segredos (ou variáveis de ambiente)

### Variáveis de Produção

```bash
# Obrigatórias (sem default — fail fast)
DB_URL=jdbc:postgresql://seupostgres:5432/traceguard
DB_USERNAME=traceguard
DB_PASSWORD=<secreto>
JWT_SECRET=<minimo 32 caracteres>

# Opcionais
SPRING_PROFILES_ACTIVE=prod
RATE_LIMIT_REDIS_ENABLED=true
REDIS_URL=redis://seuredis:6379
```

### Health Checks

```
GET /actuator/health          → UP/DOWN
GET /actuator/health/liveness → Liveness probe (K8s)
GET /actuator/health/readiness → Readiness probe (K8s)
```

### Docker (Recomendado)

```bash
docker compose -f backend/infra/docker-compose.yml up -d
```

---

## 6. Testes

```bash
cd backend/traceguard-api
./mvnw test -Dmaven.repo.local=.m2/repository
```

**78 testes** distribuídos em 19 arquivos, cobrindo:
- Controllers (autorização, validação)
- Services (regras de negócio)
- Filters (JWT, API key, rate limit)
- Isolamento de tenant
- Sanitização de auditoria
- Observabilidade

---

## 7. Estrutura de Migrações

O Flyway gerencia o schema de forma incremental:

```
V1  → tenants
V2  → user_accounts
V3  → audit_events
V4  → indexes
V5  → tenant_api_keys
V6  → schema alignment
V7  → last_used_at
V8  → tenant management extensions
V9  → admin_audit_events
V10 → refresh_tokens
```

Nunca edite migrations existentes. Crie V11+, V12+, etc.

---

## 8. Troubleshooting

### "TenantContext not set"
**Causa**: A requisição chegou sem JWT nem API Key, ou a API Key é inválida.
**Solução**: Verifique o header `Authorization: Bearer <token>` ou `X-API-Key: <key>`.

### "Rate limit exceeded"
**Causa**: Muitas requisições no mesmo bucket.
**Solução**: Aguarde 60 segundos. Ajuste os limites em `SecurityConfig.java`.

### 403 Forbidden
**Causa**: O papel do usuário não tem permissão para o endpoint.
**Solução**: Verifique o papel do usuário e as regras em `SecurityConfig.java`.

---

## 9. Suporte

- Documentação completa em `context/` (13 arquivos de governança)
- Issues: https://github.com/anomalyco/traceguard-audit-platform/issues
- MIT License — use, modifique, venda sem restrições

---

*TraceGuard Audit Platform v1.0.0 — Kit de Governança para Spring Boot*
