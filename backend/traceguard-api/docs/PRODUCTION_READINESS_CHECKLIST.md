# Production Readiness Checklist

Checklist para colocar a API do TraceGuard em produção. Cada item é verificável — marque somente com
evidência, não por suposição. Referências: `OPERATIONS_RUNBOOK.md` e `SECRETS_POLICY.md`.

---

## 1. Segredos e configuração

- [ ] `JWT_SECRET` gerado aleatoriamente, com **32+ caracteres**, **único deste ambiente**
      (`openssl rand -base64 48`)
- [ ] `DB_PASSWORD` forte e diferente do valor de desenvolvimento
- [ ] `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` **vazios** em produção
- [ ] Nenhum segredo em arquivo versionado (`git grep` da seção 9 do `SECRETS_POLICY.md`)
- [ ] `backend/infra/.env` não está rastreado (`git check-ignore -v backend/infra/.env`)
- [ ] Segredos injetados pelo secret store do ambiente, não por arquivo em disco compartilhado
- [ ] `SPRING_PROFILES_ACTIVE=prod`

## 2. Exposição de rede

- [ ] **PostgreSQL e Redis não publicados na internet.** O `docker-compose.yml` mapeia `5432:5432` e
      `6379:6379` — em servidor com IP público isso expõe o banco e o cache. Remova esses mapeamentos
      (a API fala com eles pela rede interna do Compose) ou restrinja a `127.0.0.1`
- [ ] Firewall libera somente 80/443 (e 22 para administração, restrito por chave)
- [ ] Reverse proxy com **TLS** na frente da porta 8080; HTTP redirecionando para HTTPS
- [ ] Certificado válido e renovação automática testada
- [ ] Swagger UI (`/swagger-ui.html`) e Actuator avaliados: se a API for pública, restrinja o acesso

## 3. Dados

- [ ] Backup automático agendado (`pg_dump`, ver runbook seção 6) com retenção definida
- [ ] **Restauração testada** em ambiente descartável — backup não testado não é backup
- [ ] Volumes `traceguard_db_data` e `traceguard_redis_data` em disco persistente
- [ ] Alerta de espaço em disco configurado
- [ ] Retenção definida para logs de auditoria e eventos de detecção (crescimento contínuo)

## 4. Migrations

- [ ] Migrations Flyway aplicadas com sucesso em staging antes de produção
- [ ] Backup feito **imediatamente antes** do deploy que traz migration nova
- [ ] Plano de reversão conhecido: migrations são forward-only — reverter exige backup ou migration
      compensatória
- [ ] Nenhuma migration já aplicada foi editada

## 5. Segurança da aplicação

- [ ] Rate limiting ativo com Redis (`RATE_LIMIT_REDIS_ENABLED=true`) — sem Redis o limitador cai para
      memória local e **não é compartilhado entre instâncias**
- [ ] RBAC conferido nos endpoints administrativos (4 papéis: SUPER_ADMIN, TENANT_ADMIN, OPERATOR, AUDITOR)
- [ ] API keys de tenant com escopo mínimo e revogação testada
- [ ] Multi-tenancy: tentativa de acesso cruzado entre tenants retorna 403/404 (teste manual ou automatizado)
- [ ] Detection engine com regras ativas e canal de notificação (Slack/e-mail) testado
- [ ] Correlation ID presente nos logs das requisições
- [ ] `dependency-review` (CI) sem vulnerabilidades altas em aberto
- [ ] Security headers ativos (HSTS, X-Content-Type-Options, X-Frame-Options, CSP conforme aplicável)

## 6. Observabilidade

- [ ] `/actuator/health/readiness` monitorado externamente, com alerta de indisponibilidade
- [ ] Logs com retenção e rotação configuradas
- [ ] `restart: unless-stopped` efetivo (o compose já define) e testado após reboot da máquina
- [ ] Métricas de erro 5xx acompanhadas

## 7. Operação

- [ ] Runbook revisado e comandos testados no ambiente real (`OPERATIONS_RUNBOOK.md`)
- [ ] Rollback ensaiado pelo menos uma vez
- [ ] Responsável definido para incidentes e canal de comunicação
- [ ] Janela de manutenção combinada para mudanças que invalidam sessão (rotação de `JWT_SECRET`)

## 8. CI/CD

- [ ] Workflow `traceguard-backend-ci` **verde** no branch padrão (`master`)
- [ ] Proteção de branch no `master` exigindo o check do CI antes de merge
- [ ] Dependabot ativo para dependências e GitHub Actions

## 9. Conformidade (LGPD)

A plataforma registra eventos de auditoria e detecção que podem conter **dados pessoais** (identificador de
usuário, endereço IP, user agent, horário). Portanto:

- [ ] Base legal e finalidade documentadas para o tratamento desses registros
- [ ] Prazo de retenção definido e implementado (descarte automático ou rotina manual)
- [ ] Procedimento para atender requisição de titular (acesso, correção, eliminação)
- [ ] Acesso aos registros restrito por papel e registrado (o próprio `adminaudit` cobre parte disso)
- [ ] Contrato/aviso de privacidade com clientes cobrindo o tratamento de dados pela plataforma

---

## Critério de saída

Considere pronto para produção quando **todos os itens das seções 1 a 6** estiverem marcados com evidência e
as seções 7 a 9 tiverem responsável e prazo definidos. Itens não aplicáveis devem ser marcados como
"N/A" com justificativa escrita — não deixados em branco.
