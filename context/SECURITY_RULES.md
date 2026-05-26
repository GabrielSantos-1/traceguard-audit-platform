# SECURITY_RULES.md
> Documento de maior prioridade do kit. Governa design, implementação, revisão, testes, deploy e continuidade.

---

## Princípios fundamentais

| # | Princípio |
|---|---|
| 1 | Nunca confiar em input externo — validar servidor sempre |
| 2 | Segurança é projetada antes da implementação |
| 3 | Toda operação tem escopo de acesso explícito |
| 4 | Menor privilégio é o padrão, não a exceção |
| 5 | Logs ajudam a investigar sem expor segredo |
| 6 | Falhas devem degradar com segurança |
| 7 | Dependência vulnerável é risco de arquitetura |
| 8 | Funcionalidade sem controle de acesso não está pronta |
| 9 | Ambiente de desenvolvimento não justifica insegurança estrutural |
| 10 | O que não pode ser auditado não deve ser considerado confiável |

---

## Mapeamento de superfície de ataque

Antes de implementar qualquer endpoint, fluxo ou integração, preencher:

| Campo | Preencher |
|---|---|
| Origem da entrada | body / query / path / header / cookie / arquivo / evento / webhook |
| Formato da entrada | JSON / form / multipart / querystring / binário |
| Dados manipulados | `<entidades / recursos>` |
| Papel / atributo exigido | `<papel ou atributo de acesso>` |
| Risco principal | `<BOLA / mass assignment / brute force / injeção / outro>` |
| Controles obrigatórios | `<validação / authn / authz / rate limit / auditoria>` |
| Evidência de teste | `<teste de autorização / pentest manual / outro>` |

### Fontes de entrada que exigem validação

- `body` — sempre validar contra schema estrito
- `query params` — sempre validar tipo, range e allowlist de campos
- `route params` — validar formato e nunca usar direto em query
- `headers` — validar quando usados para decisão de negócio
- `cookies` — nunca confiar em valor sem verificação de assinatura/sessão
- `arquivos` — validar MIME, extensão, tamanho e nome
- `eventos de fila` — tratar como entrada não confiável
- `webhooks` — validar assinatura quando disponível
- `integrações externas` — validar payload retornado, não assumir estrutura

---

## Validação de entrada

### Regras obrigatórias

- Tipo validado
- Formato validado (regex, enum, range)
- Limites de tamanho definidos (mínimo e máximo)
- Campos permitidos em allowlist explícita
- Campos inesperados rejeitados
- Validação **sempre** no servidor — frontend é UX, não segurança

### Proibições

- Objeto livre passado diretamente para ORM (`create(body)` — **proibido**)
- Autorização inferida a partir de campo enviado pelo cliente
- Ordenação e filtros por campos arbitrários sem allowlist
- Aceitar `null` onde não faz sentido de negócio

---

## Autenticação

### Método de autenticação

Escolher e registrar em `DECISIONS.md`:

| Método | Quando usar |
|---|---|
| Session + cookie | apps web tradicionais, SSR |
| JWT (access + refresh) | SPAs, APIs stateless, mobile |
| OAuth2 / OIDC | identidade federada, SSO corporativo |
| API Key | integrações M2M, webhooks, automações |
| mTLS | serviços internos críticos |

### Regras mínimas

- Credenciais **nunca** em query string
- Tokens **nunca** em `localStorage` sem proteção explícita e justificada
- Cookie com `HttpOnly`, `Secure` e `SameSite` adequados
- Access token de curta duração (ex: 15min)
- Refresh token com rotação e revogação
- Senhas usando hash forte: `bcrypt` (cost ≥ 12), `argon2id` ou `scrypt`
- MFA obrigatório para superfícies administrativas sensíveis quando o contexto justificar
- Resposta de falha de login sempre neutra (não revelar se email existe)

### Proibições

- Segredo ou chave hardcoded
- Token sem expiração
- Senha em texto puro em qualquer lugar
- Fallback oculto para autenticação mais fraca
- `remember me` eterno sem revogação

---

## Autorização

### Regra central

**Autenticado ≠ autorizado.**

Toda operação verifica obrigatoriamente:
- Papel (`role`)
- Ownership (`userId == resource.ownerId`)
- Tenant / organização quando aplicável
- Escopo da operação
- Estado atual do recurso
- Contexto da requisição

### Riscos obrigatórios a mitigar

| Risco | Controle |
|---|---|
| BOLA / IDOR | verificar ownership ou tenant antes de retornar/alterar |
| BFLA | verificar papel para cada operação, não apenas para rota |
| Escalada horizontal | impedir acesso a recurso de outro usuário no mesmo papel |
| Escalada vertical | impedir elevação de papel via parâmetro ou mass assignment |
| Enumeração | IDs não sequenciais (UUID v4/v7), respostas neutras |

### Exigência estrutural

Lógica de autorização **concentrada** em política, guard ou service dedicado.

Autorização espalhada em controller é falha estrutural — não é aceitável.

---

## Proteção contra injeção

| Tipo | Controle |
|---|---|
| SQL Injection | queries parametrizadas / ORM com binding correto |
| NoSQL Injection | validar tipo de operador; rejeitar operadores arbitrários |
| Command Injection | nunca passar input para shell; usar API nativa |
| Template Injection | nunca renderizar template com input de usuário sem sanitização |
| Path Traversal | normalizar path; rejeitar `..`; nunca servir arquivo por nome enviado pelo cliente |

---

## Proteção contra XSS

- Não usar `dangerouslySetInnerHTML` (React) sem sanitização robusta e necessidade real
- Não renderizar HTML arbitrário vindo de usuário
- Preferir texto puro em exibições de dados de usuário
- Se rich text for obrigatório: sanitização com allowlist de tags (ex: DOMPurify)
- Aplicar CSP (Content-Security-Policy) coerente com a aplicação

---

## Proteção contra CSRF

Aplicar em qualquer contexto autenticado por cookie:

- Validar `Origin` e `Referer` quando aplicável
- Token anti-CSRF sincronizado (Synchronizer Token Pattern) ou Double Submit Cookie
- Nunca aceitar alteração de estado via `GET`
- `SameSite=Strict` ou `SameSite=Lax` com análise de consequências

---

## Rate limiting e proteção contra abuso

### Superfícies obrigatórias

| Endpoint / Operação | Estratégia recomendada |
|---|---|
| Login | rate limit por IP + por email; lockout progressivo |
| Recuperação de senha | rate limit por IP + por email; resposta neutra |
| Criação de conta | rate limit por IP; captcha quando necessário |
| Formulários públicos | rate limit por IP |
| Endpoints caros (busca, relatório) | quota por identidade autenticada |
| Endpoints administrativos | rate limit mais estrito |
| APIs de integração | quota por API key |

### Estratégias complementares

- Throttling com backoff exponencial
- Lockout temporário após N tentativas
- Alertas de abuso para operações críticas

---

## JWT — regras específicas

Se JWT for usado:

- Claims mínimas: `sub`, `iat`, `exp`, `jti` (para revogação por jti)
- **Nunca** incluir dados sensíveis no payload (senha, segredo, dados de saúde)
- Validar: `issuer`, `audience`, `expiration`, `signature algorithm`
- Algoritmo assimétrico (`RS256`, `ES256`) para cenários com múltiplos serviços
- Prever rotação de chave sem downtime
- Estratégia de revogação documentada (blocklist de `jti` ou refresh token rotation)

---

## API Keys — regras específicas

Se API key for usada:

- Armazenar apenas o **hash** (SHA-256 com prefixo para lookup)
- Nunca armazenar o valor puro após emissão
- Exibir valor completo apenas uma vez ao emitir
- Suportar: escopo, expiração, revogação individual
- Registrar uso em log de auditoria

---

## Proteções OWASP obrigatórias

### OWASP Top 10 (Web)
- A01 Broken Access Control → RBAC/ABAC, ownership check, testes de autorização
- A02 Cryptographic Failures → HTTPS, hash forte, criptografia em repouso quando necessário
- A03 Injection → queries parametrizadas, validação de entrada
- A04 Insecure Design → threat modeling antes de implementar
- A05 Security Misconfiguration → headers, CORS, debug desligado em prod
- A06 Vulnerable Components → auditoria de dependências, atualizações
- A07 Auth Failures → tokens expiráveis, hash forte, MFA quando necessário
- A08 Software/Data Integrity → validar integridade de pacotes, pipeline seguro
- A09 Logging/Monitoring → logs com correlation ID, auditoria de operações críticas
- A10 SSRF → allowlist de destinos, nunca fazer requisição com URL de input direto

### OWASP API Security Top 10
- API1 BOLA → verificar ownership por operação
- API2 Broken Auth → token management correto
- API3 Broken Object Property Level Auth → allowlist de campos em update
- API4 Unrestricted Resource Consumption → paginação, rate limit, limite de payload
- API5 BFLA → verificar papel para cada função, não apenas por rota
- API6 Unrestricted Access to Sensitive Business Flows → rate limit de negócio
- API7 SSRF → allowlist de destinos
- API8 Security Misconfiguration → headers, CORS mínimo
- API9 Improper Inventory Management → documentar todos os endpoints
- API10 Unsafe Consumption of APIs → validar payload de APIs externas

---

## Logs, auditoria e rastreabilidade

### Logs técnicos — campos obrigatórios

```json
{
  "timestamp": "ISO 8601",
  "level": "INFO | WARN | ERROR",
  "service": "nome-do-serviço",
  "module": "nome-do-módulo",
  "correlationId": "req_xxx",
  "userId": "uuid (quando autenticado)",
  "action": "descrição da ação",
  "result": "success | failure",
  "durationMs": 123,
  "error": "mensagem normalizada (sem stack em prod)"
}
```

### Logs de auditoria — campos obrigatórios

```json
{
  "timestamp": "ISO 8601",
  "actor": "userId ou systemId",
  "action": "CREATE | UPDATE | DELETE | LOGIN | EXPORT | ...",
  "resource": "nome do recurso",
  "resourceId": "uuid",
  "tenantId": "uuid (quando aplicável)",
  "before": {},
  "after": {},
  "origin": "IP ou serviço",
  "correlationId": "req_xxx"
}
```

### Nunca logar

- Senha ou hash de senha
- Token de acesso ou refresh
- API key (nem parcial, exceto prefixo para lookup)
- PII além do estritamente necessário
- Payload bruto não mascarado de operação sensível
- Stack trace completo em ambiente de produção

---

## Segredos e configuração

- Nunca versionar `.env` com valores reais
- Sempre manter `.env.example` atualizado
- Usar secret manager / vault em produção (AWS Secrets Manager, HashiCorp Vault, etc.)
- Separar segredos por ambiente (dev / staging / prod)
- Remover segredos de logs, prints, respostas e mensagens de erro
- Não reutilizar credenciais entre ambientes

---

## Dependências

| Checklist | Ação |
|---|---|
| O problema pode ser resolvido com recurso nativo? | Avaliar antes de adicionar |
| A biblioteca é madura e ativa? | Verificar último release, issues, stars |
| Há CVEs conhecidos? | Verificar `npm audit`, `mvn dependency-check`, `snyk` |
| Licença é compatível? | Verificar antes de usar em produto comercial |
| Lockfile existe? | Obrigatório (`package-lock.json`, `pom.xml`, `go.sum`) |
| Dependência não usada? | Remover |

---

## Arquivos e uploads

Se houver upload de arquivo:

- Validar MIME type (verificar magic bytes, não apenas extensão)
- Validar extensão contra allowlist
- Validar tamanho máximo
- Renomear com nome gerado internamente (UUID)
- Armazenar fora de área pública ou atrás de URL assinada/controlada
- Nunca confiar no `filename` enviado pelo cliente
- Escanear se o contexto exigir (antivírus, ClamAV)
- Gerar URL de acesso temporária quando necessário

---

## Integrações externas

Para cada integração, documentar e implementar:

- Autenticação segura (API key em header, OAuth2, mTLS)
- Timeout definido (ex: 5s para leitura, 10s total)
- Retry com backoff exponencial e limite de tentativas
- Circuit breaker quando a indisponibilidade impacta fluxo crítico
- Validação do payload retornado (nunca assumir estrutura)
- Allowlist de domínios de destino (proteção SSRF)
- Credencial em vault / variável de ambiente
- Observabilidade: log de tentativa, resultado, duração

---

## Criptografia

| Decisão | Detalhes |
|---|---|
| Criptografia em trânsito | TLS 1.2+ obrigatório; TLS 1.3 preferido |
| Criptografia em repouso | avaliar por tipo de dado; documentar em `DECISIONS.md` |
| Hash de senha | `argon2id` (preferido), `bcrypt` (cost ≥ 12), `scrypt` |
| Hash de API key | SHA-256 com salt ou prefixo de lookup |
| Dados sensíveis em banco | criptografia com chave gerenciada externamente quando exigido |
| Gestão de chaves | rotação documentada; chave nunca em código |

---

## Hardening de headers HTTP

Aplicar conforme o contexto:

```http
Content-Security-Policy: default-src 'self'; ...
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
```

Cookie flags:
```
HttpOnly; Secure; SameSite=Strict (ou Lax com análise)
```

CORS:
- Nunca `Access-Control-Allow-Origin: *` em rota autenticada
- Allowlist explícita de origens permitidas
- Métodos e headers explicitamente declarados

---

## Erros — regras de resposta

O cliente recebe:
- Código HTTP correto (400, 401, 403, 404, 409, 422, 429, 500)
- Código de erro estável (`BAD_REQUEST`, `UNAUTHORIZED`, etc.)
- Mensagem segura para exibição
- `correlationId` para rastreabilidade

O cliente **nunca** recebe:
- Stack trace
- Query SQL interna
- Segredo ou credencial
- Informação que permita enumeração

---

## Ambientes e deploy

- Dev, staging e prod segregados
- Banco de produção nunca compartilhado com dev
- Menor privilégio para credenciais de aplicação
- Debug, profiling e ferramentas de desenvolvimento desligados em prod
- Feature flags e configs sensíveis auditáveis
- Revisão de misconfiguration como etapa obrigatória do release

---

## Gate mínimo de produção

**Nenhum sistema vai para produção sem atender todos os itens abaixo:**

- [ ] Autenticação definida, implementada e testada
- [ ] Autorização testada para cada operação crítica
- [ ] Validação server-side em todas as entradas
- [ ] Segredos fora do código e do repositório
- [ ] Logs estruturados com correlation ID
- [ ] Rate limiting em todas as superfícies expostas
- [ ] Resposta de erro segura (sem stack trace, sem dado interno)
- [ ] Checklist de dependências vulneráveis executado
- [ ] Headers de segurança configurados
- [ ] CORS mínimo configurado
- [ ] Checkpoint técnico atualizado em `PROJECT_STATE.md`
- [ ] Ao menos um teste cobrindo autorização crítica

---

## Regra final

Quando houver dúvida entre UX, prazo ou conveniência versus segurança:

**Priorizar segurança e registrar o trade-off em `DECISIONS.md`.**
