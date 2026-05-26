# API_CONTRACTS.md
> Contratos consolidados do TraceGuard ate o Bloco 9.

## Regras globais

- Sucesso:
  - `success=true`
  - `data=<payload>`
  - `meta.correlationId` presente
- Erro:
  - `success=false`
  - `error.code`, `error.message`, `error.timestamp`, `error.path`, `error.correlationId`
- Sem stacktrace para cliente.
- Tenant de autoridade nunca vem do request para decisao final.

## Semantica HTTP de seguranca

- `401 Unauthorized` => autenticacao falha:
  - sem credencial
  - JWT invalido/expirado/malformado
  - API key invalida/expirada/revogada/desabilitada
- `403 Forbidden` => autenticado, mas negado:
  - role sem permissao
  - tenant suspenso (enforcement operacional)

## Contratos de auth (`/api/v1/auth/*`)

### POST `/api/v1/auth/login`
- Publico
- Request: `email`, `password`
- Response (inalterado):
  - `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `refreshExpiresIn`, `user`

### POST `/api/v1/auth/refresh`
- Publico
- Request: `refreshToken`
- Response (inalterado):
  - `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `refreshExpiresIn`

### POST `/api/v1/auth/logout`
- Publico (conforme fluxo atual)
- Request: `refreshToken`
- Response: `204 No Content`

## Contrato de admin audit query

### GET `/api/v1/admin/audit-events`
- Roles permitidas: `SUPER_ADMIN`, `TENANT_ADMIN`
- Filtros allowlist: `tenantId`, `adminId`, `action`, `resourceType`, `resourceId`, `from`, `to`, `page`, `size`, `sort`
- Paginacao obrigatoria; `size` maximo `100`
- Response de sucesso inalterado:
  - envelope padronizado com lista paginada de `AdminAuditEventResponse`
  - `detailsJson` sanitizado

## Contratos administrativos adicionais

### API keys administrativas
- `POST /api/v1/admin/api-keys` (`SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`)
- `GET /api/v1/admin/api-keys` (`SUPER_ADMIN`, `TENANT_ADMIN`, `AUDITOR`, `OPERATOR`)
- `PATCH /api/v1/admin/api-keys/{id}/revoke` (`SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`)
- Segredo bruto retornado apenas na criacao.

### Tenants administrativos
- `POST /api/v1/admin/tenants` (`SUPER_ADMIN`)
- `GET /api/v1/admin/tenants` (`SUPER_ADMIN`, `AUDITOR`)
- `PATCH /api/v1/admin/tenants/{id}` (`SUPER_ADMIN`)

## Integracao por API key (`/api/v1/audit/events`)

### POST `/api/v1/audit/events`
- Credencial: `X-API-Key`
- Scope exigido: `AUDIT_WRITE`
- Response: `CreateAuditEventResponse` (`id`)

### GET `/api/v1/audit/events`
- Credencial: `X-API-Key`
- Scope exigido: `AUDIT_READ`
- Sort allowlist: `occurredAt`, `severity`, `action`, `userId`
- `size` maximo: `100`

## Rate limiting

- Status de bloqueio: `429`
- Headers mantidos:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `Retry-After`
- Backend: Redis com fallback in-memory
- Identidade de quota:
  - admin: `tenantId:userId`
  - audit autenticado: `tenantId`
  - fallback para IP quando identidade confiavel nao existe

## Contrato operacional (Bloco 9)

### Perfis de ambiente
- `dev`, `test`, `staging`, `prod` segregados por `application-<profile>.yml`.
- `staging` e `prod` exigem segredos externos (fail-fast em bootstrap sem `JWT_SECRET`, `DB_*`, `REDIS_URL`).

### Actuator
- Endpoints expostos em `staging/prod`: apenas `health` e `info`.
- Readiness/liveness ativos via `management.endpoint.health.probes.enabled=true`.
- Endpoint operacional de readiness: `GET /actuator/health/readiness`.

## Superficie de ataque (resumo)

- BFLA/BOLA mitigados por JWT+RBAC e isolamento por tenant no backend.
- Abuso de volume mitigado por rate limit distribuido.
- Vazamento em trilha administrativa mitigado por sanitizacao dupla camada.
- Credencial comprometida mitigada por revogacao/expiracao e fail-closed.
- Misconfiguration operacional reduzida por perfis segregados, actuator minimo e segredos externalizados.
