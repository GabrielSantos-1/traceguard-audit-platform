# BOOTSTRAP_BLUEPRINT.md
> Procedimento oficial para iniciar qualquer projeto do zero. Bootstrap incorreto = projeto torto desde o início.

---

## Regra central

Bootstrap não é "criar projeto e sair codando".

Bootstrap correto significa preparar a fundação para que o restante não nasça com dívida técnica, falhas de segurança ou arquitetura que precisará ser reescrita.

---

## Ordem obrigatória de bootstrap

### Etapa 0 — Enquadramento

Antes de qualquer código ou decisão técnica:

- [ ] `PROJECT_BRIEF.md` preenchido
- [ ] Criticidade do sistema definida
- [ ] Dados classificados por sensibilidade
- [ ] Perfis de acesso mapeados
- [ ] Escopo do MVP definido
- [ ] Ameaças iniciais identificadas

**Gate:** sem estas informações, nenhuma decisão técnica é confiável.

---

### Etapa 1 — Arquitetura

- [ ] Camadas definidas (domínio, aplicação, infraestrutura, interface)
- [ ] Árvore de diretórios proposta
- [ ] Módulos principais identificados
- [ ] Fluxo de dependências validado (sem violação)
- [ ] Escolha de stack justificada em `DECISIONS.md`

**Gate:** arquitetura aprovada antes de qualquer código.

---

### Etapa 2 — Contratos e segurança

- [ ] Entidades principais modeladas
- [ ] DTOs de entrada e saída por operação
- [ ] Contratos documentados em `API_CONTRACTS.md`
- [ ] Estratégia de autenticação definida e registrada
- [ ] Estratégia de autorização definida e registrada
- [ ] Superfície de ataque inicial mapeada
- [ ] Regras de rate limiting definidas
- [ ] Operações que exigem auditoria identificadas

**Gate:** toda entrada tem contrato; toda operação crítica tem ameaça mapeada.

---

### Etapa 3 — Base técnica

- [ ] Projeto inicial criado com estrutura de diretórios correta
- [ ] Ambiente local funcionando
- [ ] Build / typecheck / lint / formatter configurados
- [ ] `.env.example` criado e `.env` no `.gitignore`
- [ ] Helpers de resposta padronizada (envelope + erro)
- [ ] Módulo de logging com correlation ID
- [ ] Módulo de validação de schema
- [ ] Base de autenticação funcionando (mesmo que mínima)

**Gate:** projeto sobe localmente; build passa; segredos fora do código.

---

### Etapa 4 — Infraestrutura

- [ ] Schema inicial e primeira migration
- [ ] Repositórios com interface na aplicação
- [ ] Clients externos com timeout e retry
- [ ] Integração com secret manager / variáveis de ambiente
- [ ] Observabilidade básica funcionando (logs + correlation ID)

**Gate:** persistência funcional; integrações com error handling.

---

### Etapa 5 — Primeiros fluxos

- [ ] Ao menos 1 operação crítica ponta a ponta implementada
- [ ] Validação de schema aplicada
- [ ] Autenticação e autorização aplicadas
- [ ] Testes mínimos para o fluxo (unitário + autorização)
- [ ] Auditoria funcionando quando aplicável

**Gate:** fluxo crítico funcional, testado e com controles de segurança.

---

### Etapa 6 — Checkpoint de bootstrap

- [ ] `TASKS.md` atualizado com tarefas da próxima fase
- [ ] `PROJECT_STATE.md` com checkpoint de bootstrap
- [ ] `DECISIONS.md` com decisões desta fase
- [ ] `BACKLOG.md` ajustado para próxima fase real

**Gate:** bootstrap concluído; continuação possível sem depender de memória.

---

## Checklist completo de bootstrap

### Governança
- [ ] Objetivo do sistema definido em `PROJECT_BRIEF.md`
- [ ] Escopo do MVP claro
- [ ] Perfis de acesso mapeados
- [ ] Ameaças iniciais identificadas

### Arquitetura
- [ ] Árvore de diretórios definida e aprovada
- [ ] Camadas separadas sem violação
- [ ] Stack justificada em `DECISIONS.md`
- [ ] Decisão de authN/authZ registrada

### Segurança
- [ ] Contratos iniciais definidos
- [ ] Superfície de ataque mapeada
- [ ] Segredos fora do código
- [ ] Rate limiting previsto nas superfícies expostas

### Técnico
- [ ] Projeto sobe localmente sem erro
- [ ] Build / typecheck / lint executáveis
- [ ] Observabilidade mínima pronta (logs + correlation ID)
- [ ] Base de auth e validação existem

### Continuidade
- [ ] Checkpoint de bootstrap criado em `PROJECT_STATE.md`
- [ ] Próximas tarefas em `TASKS.md`
- [ ] `BACKLOG.md` aponta fase real a seguir

---

## Entregáveis mínimos por tipo de projeto

### API / backend
- Rotas base com envelope de resposta
- Contrato de erro padronizado
- Autenticação base (JWT ou sessão)
- Autorização base (guard/policy)
- Persistência inicial com migration
- Rate limiting nas superfícies expostas
- Logging com correlation ID

### SaaS / full-stack
- Estrutura backend + frontend separada por camadas
- Auth base (login, logout, refresh)
- Políticas por papel e atributo
- Formulários com validação dupla (client + server)
- Páginas/handlers base
- Logs estruturados

### App interno / CRUD admin
- RBAC mínimo por papel
- Auditoria para alterações administrativas
- Filtros e paginação com allowlist
- Proteção de rotas por papel
- Trilha de checkpoints desde o início

### Worker / integração / webhook
- Validação de payload de entrada
- Autenticação de origem (HMAC signature, API key)
- Retry com backoff e limite de tentativas
- Idempotência definida
- Observabilidade e correlation ID
- Error handling sem perda de mensagem

---

## Proibições no bootstrap

- Inventar arquitetura durante a implementação
- Escolher dependência sem justificativa em `DECISIONS.md`
- Pular modelagem de contrato
- Deixar autenticação "para depois"
- Ignorar logs e rate limit em superfícies expostas
- Encerrar bootstrap sem checkpoint técnico
- Hardcoded secrets de qualquer tipo
- Colocar regra de negócio no controller por "facilidade"

---

## Critério de aceite do bootstrap

O bootstrap é concluído quando:

- [ ] Base organizada por camadas sem violação
- [ ] Segurança inicial considerada e mapeada
- [ ] Existe ao menos uma trilha de execução crítica bem desenhada
- [ ] Documentação permite continuação sem depender da conversa
- [ ] `BACKLOG.md` aponta a próxima fase real
- [ ] `PROJECT_STATE.md` tem checkpoint de bootstrap

---

## Regra final

Se o bootstrap sair mal, o projeto inteiro pagará a conta.

Custo de corrigir arquitetura ou segurança no meio da implementação é 5–10x maior do que fazer certo no início.
