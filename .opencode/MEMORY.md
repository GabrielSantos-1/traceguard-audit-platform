# TraceGuard — Project Memory

> Atualizado: 26/05/2026

## Descrição
Plataforma de User Activity Monitoring (UAM) + Security Detection Engine + Dashboard Premium. Java 21, Spring Boot 3.4.3, PostgreSQL.

## Arquitetura
- **Camadas**: monitoring/activity (UAM), monitoring/detection (detection engine), monitoring/notification (alertas), security (headers)
- **Cadeia de Filters**: SecurityHeadersFilter → RequestCorrelationLoggingFilter → BlockedIpFilter → UserActivityFilter → JwtAuthFilter → ApiKeyFilter → RateLimitFilter → AuthorizationFilter
- **Detection Engine**: @Scheduled(fixedDelay=60000), coleta alerts de todas as regras, saveAll + notifyAlerts
- **Notificação**: @Async, agrupa por tenant, enfileira por canal configurado
- **Frontend**: Dashboard SPA inline (972 linhas), Chart.js, dark/light theme, 8 seções

## Decisiones
- NotificationService chamado pelo DetectionEngine após salvar alerts (não event-driven)
- BlockedIpFilter antes de UserActivityFilter para não logar requests bloqueados
- Null-check defensivo em todos os NotificationChannel.send()
- Slack via HttpClient nativo, Email via JavaMailSender (sem frameworks extra)
- Config de notificação por tenant em tabela própria JSONB
- SecurityHeadersFilter como primeiro filtro da cadeia (antes de tudo)
- Dashboard como SPA inline (sem React) para deploy simplificado
- CSP com 'unsafe-inline' e 'unsafe-eval' por Tailwind (trade-off aceito para SPA)
- Chart.js via CDN (leve, 4.4.7, sem dependências)

## Padrões
- OncePerRequestFilter para filters de segurança
- @Async para operações I/O-bound (notificação, log de atividade)
- Severity filter: alerta só notifica se severity >= min_severity
- Rule filter: regex match no nome da regra
- CSS custom properties para theming (dark/light)
- Chart.js sem framework wrapper (instanciação direta em canvas)

## Próxima Fase
- Fase 5: Publicação (git filter-repo, Gumroad, divulgação)
