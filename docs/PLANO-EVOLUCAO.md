# TraceGuard — Plano de Evolução

> Salvo em: 26/05/2026
> Produto: TraceGuard Security Platform

## Direção

Evoluir de template de auditoria genérico ($19) para plataforma de **User Activity Monitoring + Security Detection** com modelo de assinatura mensal.

## Modelo de Negócio

| Tier | Preço | Retenção | Admins | Tenants | Alerts | Detection |
|------|-------|----------|--------|---------|--------|-----------|
| **Guard** 🛡️ | $0/mês | 7 dias | 1 | 1 | ❌ | ❌ |
| **Shield** 🛡️🛡️ | $29/mês | 30 dias | 5 | 3 | ✅ Slack | Básico |
| **Fortress** 🛡️🛡️🛡️ | $99/mês | 90 dias | ∞ | ∞ | ✅ Todos | Completo |

## Fases

### Fase 1 — UAM Core ✅
Rastrear TODA ação de usuário no sistema:
- Quem fez o quê (userId + role + nome)
- O que mudou (before → after com diff automático)
- Quando (timestamp + timezone)
- De onde (IP + user-agent)
- O que tentou fazer (ações com falha: 401, 403, 429)
- Sucesso vs falha
- Timeline por usuário
- Frontend: Activity Timeline + User Profile

### Fase 2 — Security Detection Engine ✅
Motor de regras de detecção de ameaças:
- Brute force: N tentativas em X minutos → alerta → auto-block
- Geo anomaly: login de 2 países em <30min
- Time anomaly: acesso fora do horário padrão do usuário
- Permission escalation: tentativa de ação acima do role
- API key abuse: key usada de IP nunca antes visto

### Fase 3 — Alertas + Resposta Automática ✅
- ✅ Slack via webhook HTTP (attachments com severidade)
- ✅ Email via JavaMailSender (template HTML inline)
- ✅ Regras configuráveis (min_severity, rule_filter regex)
- ✅ Notificação multicanal por tenant (NotificationConfig JSONB)
- ✅ Auto-block de IP em tempo real (BlockedIpFilter)
- ✅ Integração DetectionEngine → NotificationService (@Async)
- ✅ 24 testes: BlockedIpFilter (7), NotificationService (13), SlackChannel (4)

### Fase 4 — Compliance + Frontend Premium ✅
- ✅ Dashboard premium com Chart.js (timeline 24h, severity doughnut)
- ✅ KPIs com delta percentual (eventos, alerts, usuários, tenants)
- ✅ 8 seções: Dashboard, Alerts, Activity, Audit, Blocked IPs, Tenants, API Keys, Integrations
- ✅ Dark/Light theme toggle
- ✅ Export CSV de relatório de segurança
- ✅ SecurityHeadersFilter: CSP, HSTS, X-Content-Type-Options, Referrer-Policy, Permissions-Policy, X-Frame-Options

### Fase 5 — Publicação
- Git filter-repo (purge [senha-de-dev-removida])
- Repositório público
- Página Gumroad atualizada
- Post Dev.to + Reddit + LinkedIn

## Workflow de Checkpoint

A cada fase concluída:
1. Salvar no Obsidian (03-PROJETOS/TraceGuard/)
2. Salvar no Supermemory (tipo: architecture)
3. Atualizar PROJECT_STATE.md
4. Atualizar PLANO-EVOLUCAO.md
5. Consolidar via evolve_memory
