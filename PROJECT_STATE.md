# TraceGuard — Project State

> Atualizado: 26/05/2026
> Build: `mvn test` → 141 tests, 0 failures, 0 errors
> Fase 4 concluída

## Fases Concluídas

### Fase 1 — UAM Core ✅
Rastreamento completo de ações de usuário:
- Flyway V12: tabela `user_activity_log` (12 colunas, 5 índices)
- `UserActivityEvent`, `UserActivityRepository`, `UserActivityService` (`@Async`)
- `UserActivityFilter` (OncePerRequestFilter com StatusCaptureWrapper)
- `UserActivityController` (GET `/api/v1/admin/activity`)
- Frontend: ActivitySection no SPA inline
- 14 testes novos (service + controller + filter)

### Fase 2 — Security Detection Engine ✅
Motor de detecção de ameaças com 5 regras:
- **Flyway V13**: `detection_alerts` + `blocked_ips` (2 tabelas, 8 índices)
- **BruteForceRule**: 5+ falhas/IP/5min → alerta HIGH + auto-block 30min
- **PermissionEscalationRule**: 5+ distinct 403s/user/5min → alerta MEDIUM
- **ApiKeyAbuseRule**: API key de IP novo → alerta HIGH
- **TimeAnomalyRule**: Atividade fora do range típico → alerta LOW
- **GeoAnomalyRule**: Login de IP novo (3+ histórico) → alerta MEDIUM
- **DetectionEngine**: `@Scheduled(fixedDelay=60000)`, tolerante a falhas
- **DetectionController**: GET/PATCH alerts, GET blocked-ips, GET count
- 22 testes novos (5 regras + service + controller)
- `@EnableScheduling` + `spring-security-test` no pom.xml

### Fase 3 — Alertas + Resposta Automática ✅
Sistema de notificação multicanal com bloqueio em tempo real:
- **Flyway V14**: `notification_configs` (JSONB, min_severity, rule_filter)
- `NotificationChannel` (interface), `SlackNotificationChannel` (HTTP POST webhook), `EmailNotificationChannel` (JavaMailSender)
- `NotificationConfig` + `NotificationConfigRepository`
- `NotificationService` (`@Async`, integrado ao `DetectionEngine`)
- `BlockedIpFilter` (OncePerRequestFilter antes do `UserActivityFilter`)
- `BlockedIpFilter` na SecurityConfig (bloqueia antes de logar atividade)
- 24 testes novos (BlockedIpFilter 7, NotificationService 13, SlackChannel 4)

### Fase 4 — Compliance + Frontend Premium ✅
Dashboard premium com segurança em todas as camadas:
- **Frontend**: SPA inline 972 linhas, Chart.js (timeline 24h + severity doughnut), dark/light theme
- **KPIs**: 4 métricas (eventos, alerts, usuários, tenants) com delta percentual
- **8 seções**: Dashboard, Alerts, Activity, Audit, Blocked IPs, Tenants, API Keys, Integrations
- **Export CSV**: relatório consolidado de segurança
- **SecurityHeadersFilter**: CSP, HSTS (1 ano), X-Content-Type-Options: nosniff, Referrer-Policy, Permissions-Policy, X-Frame-Options: DENY

## Arquitetura Atual

```
com.traceguard.monitoring/
├── activity/          # Fase 1 — UAM Core
│   ├── dto/           # ActivityQueryRequest, ActivityResponse
│   ├── UserActivityEvent.java
│   ├── UserActivityFilter.java
│   ├── UserActivityRepository.java
│   ├── UserActivityService.java
│   └── UserActivityController.java
├── detection/         # Fase 2 — Security Detection Engine
│   ├── dto/           # AlertResponse, AlertQueryRequest, BlockedIpResponse
│   ├── DetectionRule.java         (interface)
│   ├── BruteForceRule.java        (HIGH)
│   ├── PermissionEscalationRule.java (MEDIUM)
│   ├── ApiKeyAbuseRule.java       (HIGH)
│   ├── TimeAnomalyRule.java       (LOW)
│   ├── GeoAnomalyRule.java        (MEDIUM)
│   ├── DetectionEngine.java       (@Scheduled)
│   ├── DetectionAlert.java        (entity)
│   ├── BlockedIp.java             (entity)
│   ├── DetectionAlertRepository.java
│   ├── BlockedIpRepository.java
│   ├── DetectionService.java
│   └── DetectionController.java
└── notification/      # Fase 3 — Alertas + Resposta Automática
    ├── NotificationChannel.java          (interface)
    ├── NotificationConfig.java           (entity)
    ├── NotificationConfigRepository.java
    ├── NotificationService.java          (@Async)
    ├── SlackNotificationChannel.java     (webhook HTTP)
    └── EmailNotificationChannel.java     (JavaMailSender)
```

## Cadeia de Filters

```
SecurityHeadersFilter (CSP, HSTS, security headers)
  → RequestCorrelationLoggingFilter (metering + correlationId)
  → BlockedIpFilter (bloqueio em tempo real)
  → UserActivityFilter (log de atividade)
  → JwtAuthFilter (autenticação JWT)
  → ApiKeyFilter (autenticação API Key)
  → RateLimitFilter (rate limiting por bucket)
  → AuthorizationFilter (Spring Security RBAC)
```

## Próximas Fases

### Fase 5 — Publicação
- Git filter-repo (purge secrets)
- Repositório público + Gumroad
- Post Dev.to + Reddit + LinkedIn

## Configurações de Detection (application.properties)

```properties
traceguard.detection.interval-ms=60000
traceguard.detection.brute-force.threshold=5
traceguard.detection.brute-force.window-minutes=5
traceguard.detection.permission-escalation.threshold=5
traceguard.detection.permission-escalation.window-minutes=5
```
