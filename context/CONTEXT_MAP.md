# CONTEXT_MAP.md
> Mapa de governanca e continuidade do TraceGuard no checkpoint do Bloco 8.

## Ordem obrigatoria de leitura

1. `PROJECT_BRIEF.md`
2. `ARCHITECTURE.md`
3. `SECURITY_RULES.md`
4. `CODING_STANDARDS.md`
5. `API_CONTRACTS.md`
6. `BACKLOG.md`
7. `TASKS.md`
8. `PROJECT_STATE.md`
9. `DECISIONS.md`

## Modulos e responsabilidades atuais

| Modulo | Responsabilidade |
|---|---|
| `auth` | login administrativo, emissao/validacao de JWT hardened, refresh token com rotacao/revogacao/reuse detection |
| `security` | `SecurityConfig`, filtros de seguranca (`JwtAuthFilter`, `ApiKeyFilter`), tratamento padronizado de erro HTTP de seguranca |
| `ratelimit` | limitacao de taxa por bucket com Redis + fallback in-memory e headers de cota |
| `adminaudit` | persistencia e consulta da trilha administrativa com filtros, paginacao, RBAC e tenant isolation |
| `tenant` | gestao de tenants (create/list/update), status operacional (`ACTIVE/SUSPENDED`) |
| `apikey` | gestao administrativa de API keys (create/list/revoke), ciclo de vida e metadados |
| `audit` | ingestao/consulta de eventos auditados por API key e escopos |
| `common` | envelopes API, correlationId, sanitizacao utilitaria e componentes transversais |

## Integracao de seguranca (filtros -> SecurityConfig -> services)

1. `SecurityConfig` define cadeia de filtros e regras de autorizacao por rota.
2. `RequestCorrelationLoggingFilter` injeta rastreabilidade de request.
3. `JwtAuthFilter` autentica `Bearer` para `/api/v1/admin/**`, resolve principal e enforce tenant ativo (exceto `SUPER_ADMIN`).
4. `ApiKeyFilter` autentica `X-API-Key` para rotas de integracao, resolve scopes e tenant context, aplica governanca de chave e tenant.
5. `RateLimitFilter` aplica quotas por bucket com identidade confiavel (tenant/user quando disponivel).
6. Services (`AdminAuditService`, `AuditEventService`, `ApiKeyService`, `TenantService`) aplicam regras de dominio e validacoes de seguranca no backend.

## Regras de governanca cruzada

- Tenant de autoridade sempre vem de credencial autenticada, nunca de input externo.
- API key revogada/expirada/invalida nao autentica.
- Tenant suspenso e bloqueado em rotas operacionais protegidas.
- `401` e reservado para autenticacao falha; `403` para autorizacao negada/tenant bloqueado.

## Regra de encerramento de sessao

- Atualizar `PROJECT_STATE.md` e `TASKS.md`
- Registrar decisao nova em `DECISIONS.md` quando houver impacto estrutural
- Manter evidencias de validacao com comandos reais
