# DECISIONS.md
> Registro de decisoes arquiteturais, de seguranca e de produto.

---

## Regras

| # | Regra |
|---|---|
| 1 | Toda decisao relevante tem ID sequencial |
| 2 | Toda decisao registra contexto, escolha e consequencia |
| 3 | Toda reversao cita a decisao anterior |
| 4 | Escolha sem trade-off registrado e decisao fraca |
| 5 | Seguranca e autorizacao sempre exigem justificativa |

---

## Status possiveis

| Status | Significado |
|---|---|
| `proposta` | em discussao |
| `aceita` | aprovada e vigente |
| `substituida` | substituida por outra decisao (citar qual) |
| `revogada` | cancelada sem substituta (registrar motivo) |

---

## Decisoes registradas

### DECISION-001 - Arquitetura em camadas obrigatoria
- **Data:** `2026-04-14`
- **Status:** `aceita`
- **Area:** `arquitetura`

**Decisao:** manter separacao obrigatoria entre dominio, aplicacao, infraestrutura e interface.

### DECISION-002 - Seguranca como gate estrutural
- **Data:** `2026-04-14`
- **Status:** `aceita`
- **Area:** `seguranca`

**Decisao:** tratar seguranca como requisito de arquitetura desde contrato/modelagem.

### DECISION-003 - Checkpoint tecnico obrigatorio por sessao
- **Data:** `2026-04-14`
- **Status:** `aceita`
- **Area:** `processo`

**Decisao:** toda sessao encerra com atualizacao de `PROJECT_STATE.md` e `TASKS.md`.

### DECISION-004 - Hardening JWT fail-closed
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `auth`

#### Contexto
Access token precisava de garantias fortes de claims e validacao previsivel para reduzir risco de bypass.

#### Opcoes consideradas
1. Validacao parcial tolerante
2. Validacao estrita fail-closed no JWT atual
3. Migracao de protocolo completo nesta fase

#### Decisao
Adotar validacao estrita fail-closed no JWT atual, mantendo o modelo existente.

#### Consequencias
- [pro] Rejeicao segura de token invalido
- [con] Tokens fora do contrato passam a falhar imediatamente
- [risco] Erro de configuracao de chave impede bootstrap

### DECISION-005 - Sanitizacao de admin audit em dupla camada
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Decisao
Sanitizar `detailsJson` na persistencia e reforcar sanitizacao na resposta.

#### Consequencias
- [pro] Reduz risco de vazamento em armazenamento e API
- [con] Possivel perda de detalhe util para forense
- [risco] Politica de campos sensiveis exige manutencao constante

### DECISION-006 - Rate limiting distribuido com fallback seguro
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Decisao
Usar Redis como backend principal e fallback in-memory em falha, sem derrubar requests.

#### Consequencias
- [pro] Melhor consistencia multi-instancia
- [con] Degradacao de consistencia durante fallback prolongado
- [risco] Falha de Redis pode mascarar pressao se monitoramento for insuficiente

### DECISION-007 - Separacao semantica 401 vs 403
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Decisao
Padronizar `401` para autenticacao falha e `403` para autorizacao negada.

#### Consequencias
- [pro] Contrato HTTP previsivel
- [con] Necessidade de ajuste de testes legados
- [risco] Regressao se filtro capturar excecao de autorizacao indevidamente

### DECISION-008 - Governanca de API keys com fail-closed
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Contexto
Uso de API key em producao exigia ciclo de vida governado e bloqueio imediato de credencial invalida.

#### Opcoes consideradas
1. Validar apenas hash
2. Validar hash + enabled + revokedAt + expiresAt com classificacao de falha
3. Delegar governanca para etapa futura

#### Decisao
Aplicar governanca fail-closed no backend: chave so e valida quando `enabled=true`, `revokedAt=null` e `expiresAt` futuro (ou nulo), com telemetria por motivo de falha.

#### Consequencias
- [pro] Revogacao e expiracao efetivas em tempo real
- [con] Mais pontos de estado para manter consistencia
- [risco] Erro operacional de status pode bloquear tenant legitimo

### DECISION-009 - Enforcement central de tenant ACTIVE/SUSPENDED
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Decisao
Bloquear tenant nao `ACTIVE` em fluxos JWT/API key sensiveis, com `403` e observabilidade dedicada.

#### Consequencias
- [pro] Suspensao efetiva de operacao do tenant
- [con] Dependencia de sincronia do status no banco
- [risco] Tenant sem registro valido e tratado como bloqueado (fail-closed)

### DECISION-010 - Rate limit por identidade confiavel
- **Data:** `2026-04-15`
- **Status:** `aceita`
- **Area:** `seguranca`

#### Decisao
Priorizar identidade autenticada na chave de quota:
- admin: `tenantId:userId`
- audit autenticado: `tenantId`
- fallback para IP apenas quando identidade confiavel nao existe

#### Consequencias
- [pro] Menor risco de bypass/distribuicao por IP
- [con] Exige ordem de filtros consistente para contexto disponivel
- [risco] Contexto ausente em rota de audit deve ser explicitamente monitorado

### DECISION-011 - Production readiness com segregacao de perfis e fail-fast de segredos
- **Data:** `2026-04-16`
- **Status:** `aceita`
- **Area:** `operacao`

#### Contexto
Bloco 8 concluiu seguranca funcional, mas faltava gate operacional para producao (ambientes, secrets, deploy e CI).

#### Opcoes consideradas
1. Manter configuracao unica com defaults locais
2. Segregar perfis (`dev/test/staging/prod`) com segredos externos obrigatorios em `staging/prod`
3. Introduzir nova arquitetura de deploy mais complexa nesta fase

#### Decisao
Adotar opcao 2: perfis segregados, fail-fast para segredos criticos em `staging/prod`, actuator minimo em ambientes sensiveis, pipeline CI com build/test/artefato e dependency review.

#### Consequencias
- [pro] Reduz risco de security misconfiguration e vazamento de credenciais
- [pro] Melhora auditabilidade da promocao para producao
- [con] Exige disciplina operacional na provisao de variaveis por ambiente
- [risco] Sem secret manager configurado corretamente, bootstrap de staging/prod falha por design

### DECISION-012 - Gate final operacional exige bootstrap compose em banco limpo
- **Data:** `2026-04-16`
- **Status:** `aceita`
- **Area:** `operacao`

#### Contexto
No gate 10A, a API entrou em restart loop no compose por falhas de bootstrap em ambiente limpo.

#### Opcoes consideradas
1. Ignorar erro e validar apenas testes locais
2. Ajustar minimamente o artefato operacional bloqueador e revalidar compose do zero
3. Relaxar validacoes de schema para facilitar subida

#### Decisao
Adotar opcao 2. Foi aplicado ajuste minimo e auditavel na migration Flyway (`V10__create_refresh_tokens.sql`), mantendo `ddl-auto=validate` e sem afrouxar seguranca/contrato.

#### Consequencias
- [pro] Bootstrap limpo e reproduzivel em compose
- [pro] Gate final passa a detectar drift real de banco cedo
- [con] Exige reset de volume local quando credenciais do Postgres mudam
- [risco] Sem disciplina de operacao local, conflitos de estado de volume podem reaparecer

### DECISION-013 - Gate 10B nao pode ser marcado como concluido sem staging remoto real
- **Data:** `2026-04-17`
- **Status:** `aceita`
- **Area:** `operacao`

#### Contexto
O roteiro do Bloco 10B exige deploy fora da maquina de desenvolvimento. Na sessao atual, `docker context ls` mostrou apenas ambientes locais (`default` e `desktop-linux`), sem alvo remoto.

#### Opcoes consideradas
1. Marcar 10B como concluido com base apenas em validacao local controlada
2. Registrar 10B como parcial, manter evidencias locais e explicitar pendencia remota
3. Relaxar criterio do bloco para aceitar somente compose local

#### Decisao
Adotar opcao 2: manter as validacoes operacionais locais como evidencias uteis, mas classificar o bloco como **parcialmente concluido** ate execucao em staging remoto real.

#### Consequencias
- [pro] Estado do projeto permanece auditavel e aderente ao criterio do gate
- [pro] Evita falso positivo de readiness de producao
- [con] Exige nova janela operacional para validacao externa
- [risco] Sem ambiente remoto provisionado, o fechamento formal do 10B permanece bloqueado

### DECISION-014 - Bloco 10B.1 exige prerequisitos explicitos de acesso remoto
- **Data:** `2026-04-17`
- **Status:** `aceita`
- **Area:** `operacao`

#### Contexto
O 10B.1 demanda deploy real fora da maquina local, mas a sessao atual nao possui host remoto, credenciais SSH ou contexto remoto disponivel.

#### Opcoes consideradas
1. Simular remoto localmente e marcar como concluido
2. Declarar bloqueio e registrar 10B.1 como nao concluido ate acesso remoto real
3. Afrouxar criterio do bloco para aceitar apenas evidencias locais

#### Decisao
Adotar opcao 2: manter rastreabilidade do bloqueio e exigir prerequisitos remotos explicitos antes de nova tentativa.

#### Consequencias
- [pro] Mantem integridade tecnica do gate final
- [pro] Evita evidencias falsas de operacao remota
- [con] Dependencia externa de infraestrutura/credenciais
- [risco] Atraso no fechamento final ate disponibilizacao do ambiente remoto
