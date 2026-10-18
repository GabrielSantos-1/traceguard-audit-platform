# TraceGuard — Checkpoint Técnico Fase 4

> **Data:** 26/05/2026
> **Build:** `mvn test` → 141 tests, 0 failures, 0 errors, 0 skipped ✅
> **Commit:** (após este checkpoint)
> **Branch:** `master`
> **Repositório:** `git@github.com:GabrielSantos-1/traceguard-audit-platform.git`

---

## 1. Resumo Executivo

TraceGuard evoluiu de um template de auditoria genérico ($19) para uma plataforma completa de **User Activity Monitoring (UAM) + Security Detection Engine + Notificações Multicanal + Dashboard Premium** com 8 seções e 141 testes automatizados.

### O que foi entregue (Fases 1-4)

| Fase | Título | Status | Testes |
|------|--------|--------|--------|
| 1 | UAM Core | ✅ | 14 |
| 2 | Security Detection Engine | ✅ | 22 |
| 3 | Alertas + Resposta Automática | ✅ | 24 |
| 4 | Compliance + Frontend Premium | ✅ | (testes existentes + dashboard funcional) |

---

## 2. Arquitetura Geral

### 2.1 Pacotes

```
com.traceguard/
├── adminaudit/         # Admin audit trail (events + DTOs)
├── apikey/             # API key management (admin controller + DTOs)
├── audit/              # Business audit events (controller + service + DTO)
├── auth/               # JWT auth (login, refresh, logout, token service, filters)
├── common/
│   ├── api/            # Exception handler, error response writer
│   ├── bootstrap/      # Seed data initialization
│   ├── db/             # Database utilities (listener, converter)
│   ├── openapi/        # Swagger/OpenAPI config
│   ├── sanitization/   # Input sanitization
│   ├── util/           # IP resolver, UTC clock
│   └── web/            # RequestCorrelationLoggingFilter
├── dashboard/          # Dashboard KPIs + charts data (controller + DTO)
├── integration/        # Integration configs (controller + DTO)
├── monitoring/
│   ├── activity/       # UserActivity (filter, service, controller, DTO, entity, repository)
│   ├── detection/      # Detection rules engine (5 rules, engine, entities, controller, DTO)
│   └── notification/   # Notification channels (Slack, Email, service, config entity)
├── ratelimit/          # Rate limiting (filter, service, bucket registry)
├── security/           # Security config, API key filter/auth, SecurityHeadersFilter
└── tenant/             # Multi-tenancy (controller, service, entity, DTO)
```

### 2.2 Stack

| Componente | Tecnologia | Versão |
|-----------|-----------|--------|
| Java | OpenJDK | 21 |
| Spring Boot | Framework | 3.4.3 |
| Maven | Build | (wrapper) |
| PostgreSQL | Database (prod) | 16 |
| H2 | Database (dev) | (embedded) |
| Redis | Cache/Rate Limit | 7 |
| Flyway | Migrations | (integrado) |
| JWT | Auth | jjwt 0.12.6 |
| BCrypt | Password hashing | (integrado) |
| Chart.js | Dashboard gráficos | 4.4.7 (CDN) |
| Tailwind CSS | Dashboard styling | 3.4.17 (CDN) |

### 2.3 Métricas

| Métrica | Valor |
|---------|-------|
| Arquivos Java (src) | 127 |
| Arquivos de Teste | 30 |
| Flyway Migrations | 14 (V1-V14) |
| Total de arquivos do projeto | 185+ |
| Linhas do dashboard (index.html) | 971 |
| Total de testes | 141 |

---

## 3. Cadeia de Filters (Ordem de Execução)

A cadeia de segurança é construída via `SecurityConfig.apiFilterChain()` com 8 filtros `OncePerRequestFilter` encadeados:

```
Posição 0: SecurityHeadersFilter
  └─ CSP, HSTS, X-Content-Type-Options, Referrer-Policy,
     Permissions-Policy, X-Frame-Options, Server header removido

Posição 1: RequestCorrelationLoggingFilter
  └─ Correlation ID propagation + request metering (duration, method, path, status)

Posição 2: BlockedIpFilter
  └─ Bloqueio em tempo real de IPs suspeitos (consulta BlockedIpRepository)
  └─ Skips: /actuator, /swagger, /h2-console, /, /index.html, /favicon.ico
  └─ Resposta JSON padronizada: { blocked: true, correlationId, ... }

Posição 3: UserActivityFilter
  └─ Log assíncrono de TODA ação de usuário (independente de auth)
  └─ Captura: IP, user-agent, método, path, status code, userId, tenantId
  └─ Usa StatusCaptureWrapper para interceptar status code após o filter chain

Posição 4: JwtAuthFilter
  └─ Valida JWT Bearer token (assinatura, issuer, audience, claims, expiração)
  └─ Verifica tenant ativo (não suspenso)
  └─ Extrai userId, tenantId, role → SecurityContext

Posição 5: ApiKeyFilter
  └─ Valida API Key (hash, scopes AUDIT_READ/AUDIT_WRITE, tenant ativo)
  └─ Fallback: passa para próximo filter se não for API Key request

Posição 6: RateLimitFilter
  └─ 6 buckets configuráveis: login, refresh, ingest, query, admin_read, admin_write
  └─ Dual-mode: Redis (prod) → fallback in-memory (dev/H2)
  └─ Key hash: MD5 do IP (anonimizado)

Posição 7: AuthorizationFilter (Spring Security nativo)
  └─ RBAC por endpoint: SUPER_ADMIN, TENANT_ADMIN, OPERATOR, AUDITOR
  └─ Role hierarchy configurada via .hasRole() / .hasAnyRole()
```

**Detalhe de registro:** Todos os filtros exceto SecurityHeadersFilter usam `addFilterBefore(..., AuthorizationFilter.class)`. SecurityHeadersFilter e RequestCorrelationLoggingFilter usam `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` para estarem antes na cadeia.

---

## 4. API Endpoints Completos

### 4.1 Autenticação

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| POST | `/api/v1/auth/login` | Público | Login com email + password → JWT |
| POST | `/api/v1/auth/refresh` | Público | Rotação de refresh token |
| POST | `/api/v1/auth/logout` | Público | Revoga refresh token |
| PATCH | `/api/v1/auth/password` | Autenticado | (deprecated) |

### 4.2 Admin - Conta

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| PATCH | `/api/v1/admin/account/password` | Autenticado | Alterar senha |

### 4.3 Admin - Tenants

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| POST | `/api/v1/admin/tenants` | SUPER_ADMIN | Criar tenant |
| GET | `/api/v1/admin/tenants` | SUPER_ADMIN, AUDITOR | Listar tenants |
| PATCH | `/api/v1/admin/tenants/{id}` | SUPER_ADMIN | Atualizar tenant |

### 4.4 Admin - API Keys

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| POST | `/api/v1/admin/api-keys` | SUPER_ADMIN, TENANT_ADMIN, OPERATOR | Criar API key |
| GET | `/api/v1/admin/api-keys` | SUPER_ADMIN, TENANT_ADMIN, AUDITOR, OPERATOR | Listar API keys |
| PATCH | `/api/v1/admin/api-keys/{id}/revoke` | SUPER_ADMIN, TENANT_ADMIN, OPERATOR | Revogar API key |

### 4.5 Admin - Audit Events

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/admin/audit-events` | SUPER_ADMIN, TENANT_ADMIN | Query audit trail admin |

### 4.6 Audit (Business)

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/audit/events` | API Key | Query audit events |
| POST | `/api/v1/audit/events` | API Key | Ingest audit event |

### 4.7 Admin - Activity (UAM)

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/admin/activity` | SUPER_ADMIN, TENANT_ADMIN, AUDITOR | Timeline de atividade de usuários |

### 4.8 Admin - Detection Engine

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/admin/detection/alerts` | SUPER_ADMIN, TENANT_ADMIN, AUDITOR | Listar alertas de detecção |
| PATCH | `/api/v1/admin/detection/alerts/{id}/acknowledge` | (auth) | Acknowledgar alerta |
| GET | `/api/v1/admin/detection/blocked-ips` | SUPER_ADMIN, TENANT_ADMIN | Listar IPs bloqueados |
| GET | `/api/v1/admin/detection/alerts/count` | (auth) | Contagem de alertas |

### 4.9 Admin - Dashboard

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/admin/dashboard/kpi` | SUPER_ADMIN, TENANT_ADMIN, AUDITOR, OPERATOR | KPIs (eventos, alerts, users, tenants) |
| GET | `/api/v1/admin/dashboard/severity-distribution` | (same) | Distribuição por severidade |
| GET | `/api/v1/admin/dashboard/timeline` | (same) | Timeline 24h |
| GET | `/api/v1/admin/dashboard/recent-events` | (same) | Eventos recentes |
| GET | `/api/v1/admin/dashboard/critical-alerts` | (same) | Alertas críticos |

### 4.10 Admin - Integrations

| Método | Path | RBAC | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/admin/integrations` | SUPER_ADMIN, TENANT_ADMIN | Listar integrações |
| GET | `/api/v1/admin/integrations/{provider}` | SUPER_ADMIN, TENANT_ADMIN | Obter config de integração |
| PUT | `/api/v1/admin/integrations/{provider}` | SUPER_ADMIN, TENANT_ADMIN | Salvar config de integração |
| DELETE | `/api/v1/admin/integrations/{provider}` | SUPER_ADMIN | Remover integração |

### 4.11 Públicos

| Path | Descrição |
|------|-----------|
| `/` , `/index.html` | Dashboard SPA |
| `/favicon.ico` | Favicon |
| `/actuator/health/**` , `/actuator/info` | Health check |
| `/swagger-ui/**` , `/v3/api-docs/**` | OpenAPI docs |
| `/h2-console/**` | H2 console (dev) |

---

## 5. Entity Model (Camada de Dados)

### 5.1 Entidades Principais

```
Tenant (V1)               ←── pivot multi-tenant
├── id UUID, name, slug, status (ACTIVE/SUSPENDED), settings JSONB
├── createdAt, updatedAt

User (V2)                 ←── herda tenantId
├── id UUID, email (unique), passwordHash, displayName, role
├── status (ACTIVE/INACTIVE/LOCKED), tenantId (FK → Tenant)
├── createdAt, updatedAt, lastLoginAt
└── passwordChangedAt, passwordHistory JSONB (últimas 5 senhas)

AuditEvent (V3)           ←── business audit (API Key)
├── id UUID, tenantId, action, resource, resourceId
├── severity (INFO/WARN/ERROR/CRITICAL)
├── before/after JSONB, diff TEXT
├── actorId, actorEmail, sourceIp, correlationId
├── metadata JSONB, timestamp

AdminAuditEvent (V9)      ←── admin audit trail
├── id UUID, tenantId, adminId, action, resourceType
├── resourceId, before/after JSONB, diff TEXT
├── correlationId, timestamp

TenantApiKey (V5)         ←── machine-to-machine auth
├── id UUID, tenantId, keyHash, name, prefix
├── scopes TEXT[] (AUDIT_READ, AUDIT_WRITE)
├── status (ACTIVE/REVOKED), expiresAt
├── lastUsedAt, createdAt, createdBy, revokedAt, revokedBy

RefreshToken (V10)        ←── JWT rotation
├── id UUID, userId, tenantId, tokenHash
├── expiresAt, createdAt, revokedAt
├── family TEXT (para detectar reuse)

IntegrationConfig (V11)   ←── webhook/integration settings
├── id UUID, tenantId, provider (SLACK/WEBHOOK/EMAIL)
├── config JSONB, enabled BOOLEAN, createdAt, updatedAt

UserActivityEvent (V12)   ←── UAM (todo request)
├── id UUID, tenantId, userId, role, userEmail
├── action (CREATE/READ/UPDATE/DELETE/ERROR), method, path
├── statusCode, durationMs, sourceIp, userAgent
├── requestBody/responseBody TEXT, correlationId
├── timestamp, timezone

DetectionAlert (V13)      ←── detecção de ameaças
├── id UUID, tenantId, ruleName, severity
├── description, sourceIp, userId, endpoint
├── metadata JSONB, timestamp, acknowledged BOOLEAN, acknowledgedAt, acknowledgedBy

BlockedIp (V13)           ←── IPs bloqueados automaticamente
├── id UUID, ipAddress, tenantId, ruleName
├── reason, blockedAt, expiresAt, active BOOLEAN

NotificationConfig (V14)  ←── configuração de notificação por tenant
├── id UUID, tenantId, channel (SLACK/EMAIL)
├── config JSONB (webhook_url, to, from, etc.)
├── minSeverity (LOW/MEDIUM/HIGH/CRITICAL)
├── ruleFilter TEXT (regex opcional)
├── enabled BOOLEAN, createdAt, updatedAt
```

### 5.2 Flyway Migrations (14 total)

| Migration | Descrição |
|-----------|-----------|
| V1 | Create tenants |
| V2 | Create users |
| V3 | Create audit_events |
| V4 | Indexes |
| V5 | Create tenant_api_keys |
| V6 | Align tenant_api_keys schema |
| V7 | Add api_key last_used_at |
| V8 | Extend tenants for admin management |
| V9 | Create admin_audit_events |
| V10 | Create refresh_tokens |
| V11 | Create integration_configs |
| V12 | Create user_activity_log |
| V13 | Create detection tables (detection_alerts + blocked_ips) |
| V14 | Create notification_configs |

---

## 6. Security Detection Engine

### 6.1 Regras de Detecção

| Regra | Classe | Severidade | Gatilho |
|-------|--------|-----------|---------|
| Brute Force | `BruteForceRule` | HIGH | N tentativas em X minutos por IP |
| Permission Escalation | `PermissionEscalationRule` | MEDIUM | Distinct endpoints com 403 |
| API Key Abuse | `ApiKeyAbuseRule` | HIGH | Key usada de path suspeito |
| Time Anomaly | `TimeAnomalyRule` | LOW | Acesso fora de janela horária |
| Geo Anomaly | `GeoAnomalyRule` | MEDIUM | 2 países diferentes em <30min |

### 6.2 Mecanismo

```
@Scheduled(fixedDelay = 60000) a cada 60s:
1. Query todas as regras (List<DetectionRule>)
2. Cada regra: evaluate(userActivityEvents) → List<DetectionAlert>
   - Individual try/catch por regra (falha de uma não derruba as outras)
3. saveAll(detectionAlertRepository) em lote
4. notifyAlerts(detectionAlerts) via NotificationService (@Async)
```

### 6.3 In-Memory Tracking (por regra)

| Regra | Estrutura | Limite |
|-------|-----------|--------|
| BruteForceRule | `ConcurrentHashMap<String, List<AttemptInfo>>` (IP → tentativas) | 100 por IP |
| PermissionEscalationRule | `ConcurrentHashMap<String, Set<String>>` (user@tenant → endpoints) | - |
| ApiKeyAbuseRule | `ConcurrentHashMap<String, Set<String>>` (key → paths) | - |
| TimeAnomalyRule | `ConcurrentHashMap<String, List<ZonedDateTime>>` (user@tenant → timestamps) | 100 por user |
| GeoAnomalyRule | `ConcurrentHashMap<String, Set<String>>` (user@tenant → países) | - |

---

## 7. Notification System

### 7.1 Canais

| Canal | Classe | Transporte | Formato |
|-------|--------|-----------|---------|
| Slack | `SlackNotificationChannel` | HttpClient POST | Attachments com severity emoji |
| Email | `EmailNotificationChannel` | JavaMailSender | HTML table inline |

### 7.2 Fluxo

```
DetectionEngine (coleta alerts)
  → saveAll() em lote no banco
  → NotificationService.notifyAlerts(alerts) (@Async)
    → Agrupa alerts por tenantId
    → Para cada tenant: busca NotificationConfigs habilitadas
      → Filtra por minSeverity (alert.severity >= config.minSeverity)
      → Filtra por ruleFilter (regex match no nome da regra)
      → Dispatch para canal correspondente (SlackChannel.send() / EmailChannel.send())
```

---

## 8. Dashboard Premium (Fase 4)

### 8.1 Seções

| Seção | Descrição |
|-------|-----------|
| **Dashboard** | KPIs (eventos 24h, alertas ativos, usuários, tenants) + delta percentual |
| | Timeline 24h (Chart.js line chart) |
| | Severity Distribution (Chart.js doughnut) |
| | Eventos Recentes + Alertas Críticos |
| **Alerts** | Tabela filtrável de alertas de detecção |
| **Activity** | Timeline de atividade de usuários |
| **Audit** | Audit trail administrativo |
| **Blocked IPs** | Lista de IPs bloqueados |
| **Tenants** | Gerenciamento de tenants |
| **API Keys** | Gerenciamento de chaves de API |
| **Integrations** | Configuração de webhooks/integrações |

### 8.2 Stack do Frontend

| Componente | Origem |
|-----------|--------|
| HTML/CSS/JS | Inline em `static/index.html` |
| Tailwind CSS | CDN (3.4.17) |
| Font Awesome | CDN (6.7.2) free-solid |
| Chart.js | CDN (4.4.7) |
| Tema | CSS custom properties (dark/light toggle) |
| Navegação | Hash-based SPA com transições CSS |
| Export | CSV gerado no cliente (sem jsPDF) |

### 8.3 Segurança

| Header | Valor |
|--------|-------|
| `Content-Security-Policy` | default-src 'self'; script-src 'unsafe-inline' 'unsafe-eval' cdnjs.cloudflare.com; style-src 'unsafe-inline' cdnjs.cloudflare.com; font-src cdnjs.cloudflare.com; connect-src 'self' /api/v1/...; img-src 'self' data:; |
| `Strict-Transport-Security` | max-age=31536000; includeSubDomains |
| `X-Content-Type-Options` | nosniff |
| `Referrer-Policy` | strict-origin-when-cross-origin |
| `Permissions-Policy` | camera=(), microphone=(), geolocation=() |
| `X-Frame-Options` | DENY |
| Server header | Removido |

---

## 9. Testes (141 Total)

### 9.1 Distribuição por Pacote

| Pacote | Testes | Arquivos |
|--------|--------|----------|
| `auth` | 28 | 5 |
| `security` | 17 | 2 |
| `ratelimit` | 8 | 3 |
| `monitoring/activity` | 17 | 3 |
| `monitoring/detection` | 25 | 5 |
| `monitoring/notification` | 17 | 2 |
| `adminaudit` | 13 | 3 |
| `apikey` | 2 | 1 |
| `audit` | 5 | 2 |
| `tenant` | 4 | 2 |
| `common/web` | 2 | 1 |
| **Total** | **141** | **30** |

### 9.2 Principais Test Files

| Arquivo | Tests | O que cobre |
|---------|-------|-------------|
| `JwtTokenServiceTest` | 10 | Geração, validação, claims, refresh rotation |
| `SecurityHttpTest` | 10 | Fluxo completo auth + RBAC + filtros |
| `UserActivityServiceTest` | 9 | Persistência e log assíncrono |
| `RefreshTokenServiceTest` | 7 | Refresh token lifecycle, reuse detection |
| `BlockedIpFilterTest` | 7 | Bloqueio real-time, skip paths, JSON response |
| `ApiKeyFilterTest` | 7 | Validação de API key, scopes, fallthrough |
| `AuthControllerTest` | 6 | Login, refresh, logout endpoints |
| `AdminAuditServiceTest` | 6 | Admin audit persistence |
| `NotificationServiceTest` | 13 | Notificação multicanal, filtros, agrupamento |
| `BruteForceRuleTest` | 5 | Threshold, auto-block, reset |

### 9.3 Frameworks de Teste

| Framework | Uso |
|-----------|-----|
| JUnit 5 | Base |
| Mockito | Mocking de dependências |
| Spring Boot Test | @SpringBootTest para integração |
| MockMvc | Testes de controller HTTP |

---

## 10. Configuração de Build e Deploy

### 10.1 Perfis Spring

| Perfil | Database | Redis | Docker |
|--------|----------|-------|--------|
| `dev,h2` | H2 em memória | In-memory | Não necessário |
| `dev` | PostgreSQL (`localhost:5432`) | In-memory | Sim (db service) |
| `prod` | PostgreSQL (env vars) | Redis (env vars) | Sim (full stack) |

### 10.2 Docker Compose

```yaml
services:
  db:    postgres:16     → porta 5432, volume persistente
  redis: redis:7-alpine  → porta 6379, appendonly yes
  api:   Build local     → porta 8080, depende de db+redis healthy
```

---

## 11. Histórico de Commits

| Commit | Mensagem |
|--------|----------|
| `2024e0b` | feat: complete TraceGuard security audit platform |
| `fefc665` | Refactor README and improve Docker security configuration |
| `f1c25d5` | Initial commit (clean, no secrets, no build artifacts) |
| `30172ed` | Hardening API key scopes + JDK21 Mockito fix |

> ✅ **Resolvido em 18/10/2026:** o histórico foi reescrito com `git filter-repo` e a senha não existe em nenhum commit. Os hashes desta tabela referem-se ao histórico **anterior** à reescrita.

---

## 12. Próximos Passos (Fase 5)

| Tarefa | Prioridade | Descrição |
|--------|-----------|-----------|
| ✅ Purge da senha de dev | Concluída | `git filter-repo` executado em 18/10/2026 |
| Push público | 🔴 Crítica | `git push origin --force --all` |
| Git tags | 🟡 Média | Criar tag v1.0.0 |
| CI/CD | 🟡 Média | Verificar GitHub Actions |

---

## 13. Glossário

| Termo | Definição |
|-------|-----------|
| UAM | User Activity Monitoring — rastreamento de toda ação de usuário |
| RBAC | Role-Based Access Control — 4 roles (SUPER_ADMIN, TENANT_ADMIN, OPERATOR, AUDITOR) |
| Correlation ID | UUID único por request para tracing de ponta a ponta |
| Severity | Nível de severidade: LOW, MEDIUM, HIGH, CRITICAL |
| Tenant | Unidade de isolamento multi-tenant (dados completamente separados por tenantId) |
| BlockedIp | IP bloqueado automaticamente por regra de detecção |
| Notification Channel | Canal de notificação (Slack webhook ou Email) configurado por tenant |
| JSONB | Coluna JSON binária no PostgreSQL (armazena configs, metadata, diffs) |
