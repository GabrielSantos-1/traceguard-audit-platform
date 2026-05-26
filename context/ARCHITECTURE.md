# ARCHITECTURE.md
> Define a arquitetura oficial do projeto. Nenhum código definitivo é escrito antes deste documento estar preenchido.

---

## Princípios arquiteturais

| # | Princípio |
|---|---|
| 1 | Segurança é requisito estrutural, não etapa final |
| 2 | Domínio não depende de framework, banco ou HTTP |
| 3 | Infraestrutura não define regra de negócio |
| 4 | Interface não contém regra crítica |
| 5 | Cada módulo tem responsabilidade única e clara |
| 6 | Toda integração externa é tratada como não confiável |
| 7 | Nenhum acoplamento circular é permitido |
| 8 | Toda fronteira de entrada valida, autentica e autoriza |
| 9 | Abstração só entra quando há ganho real e evidência |
| 10 | Monólito modular bem desenhado é preferível a fragmentação prematura |

---

## Camadas obrigatórias

### Domínio
Contém entidades, value objects, regras de negócio puras, políticas, serviços de domínio e eventos de domínio.

**Restrição:** não pode depender de framework, banco, UI ou HTTP. Sem anotações de ORM. Sem imports de infraestrutura.

### Aplicação
Contém casos de uso, orquestração, DTOs de entrada e saída, portas e interfaces, regras transacionais e autorização por caso de uso.

**Restrição:** depende do domínio, mas não de detalhes concretos de infraestrutura.

### Infraestrutura
Contém banco de dados, ORM/repositórios, cache, mensageria, integrações externas, provedores de autenticação, secrets/vault e observabilidade.

**Restrição:** implementa contratos definidos pela aplicação. Não contém regra de negócio.

### Interface
Contém controllers, route handlers, presenters, páginas e telas, formulários e adapters HTTP/CLI/queue/webhook.

**Restrição:** converte entrada externa em DTO válido. Nunca executa regra de negócio crítica diretamente.

---

## Fluxo permitido de dependência

```
Interface → Aplicação → Domínio
Infraestrutura → Aplicação / Domínio (via implementação de contratos)
Domínio → (nenhuma camada externa)
```

### Violações proibidas

- Domínio chamando infraestrutura diretamente
- UI acessando banco sem passar por caso de uso
- Controller contendo regra crítica de negócio
- Validação existindo apenas no frontend
- Lógica de autorização espalhada na camada de interface

---

## Árvore de diretórios recomendada

### Backend / API (Node.js ou Java)

```
project-root/
├── docs/
├── src/
│   ├── domain/
│   │   ├── entities/
│   │   ├── value-objects/
│   │   ├── services/
│   │   ├── events/
│   │   └── policies/
│   ├── application/
│   │   ├── dtos/
│   │   │   ├── input/
│   │   │   └── output/
│   │   ├── use-cases/
│   │   ├── ports/
│   │   ├── mappers/
│   │   └── validators/
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── migrations/
│   │   │   ├── repositories/
│   │   │   └── orm/
│   │   ├── security/
│   │   │   ├── auth/
│   │   │   ├── authorization/
│   │   │   └── secrets/
│   │   ├── observability/
│   │   │   ├── logging/
│   │   │   ├── tracing/
│   │   │   └── metrics/
│   │   └── integrations/
│   │       ├── clients/
│   │       ├── queues/
│   │       └── webhooks/
│   └── interface/
│       ├── http/
│       │   ├── controllers/
│       │   ├── routes/
│       │   ├── middlewares/
│       │   └── presenters/
│       ├── cli/
│       └── jobs/
└── tests/
    ├── unit/
    ├── integration/
    └── e2e/
```

### Full-stack web (Next.js / Nuxt / equivalente)

```
project-root/
├── docs/
├── src/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interface/
│       ├── web/
│       │   ├── pages/
│       │   ├── components/
│       │   └── forms/
│       └── api/
│           ├── controllers/
│           ├── routes/
│           └── middlewares/
└── tests/
```

### Java / Spring Boot

```
src/main/java/com/company/project/
├── domain/
├── application/
├── infrastructure/
└── interface/
```

> Adaptar a árvore ao projeto real. Registrar divergências em `DECISIONS.md`.

---

## Ordem obrigatória antes de qualquer código definitivo

```
1. Árvore de diretórios proposta e aprovada
2. Mapa de módulos e responsabilidades
3. Entidades e relacionamentos
4. DTOs e contratos de API
5. Superfície de ataque por endpoint / caso de uso
6. Estratégia de autenticação e autorização
7. Decisão de dependências registrada
8. Implementação incremental por camada
```

**Se a ordem for quebrada, o trabalho deve ser refeito.**

---

## Regras de modelagem

### Entidades
- Representam o negócio, não a tabela do banco
- Possuem invariantes claras e impedem estados inválidos
- Não contêm anotações de ORM no domínio puro

### DTOs
- Explícitos — nenhum campo implícito
- `CreateDTO` e `UpdateDTO` são sempre separados
- Não expõem campos internos por acidente
- Previnem mass assignment por design

### Repositórios
- Interface definida na aplicação
- Implementação concreta na infraestrutura
- Métodos nomeados por intenção de negócio, não por query

### Casos de uso
- Um objetivo por caso de uso
- Autorização explícita dentro do caso de uso
- Sem dependência direta de framework HTTP

---

## Segurança por arquitetura

### Toda fronteira de entrada trata obrigatoriamente:
- Validação de schema e tipos
- Autenticação (quando exigida)
- Autorização (papel, ownership, tenant, escopo)
- Sanitização quando necessário
- Correlation ID para rastreabilidade
- Resposta de erro consistente e segura

### Toda operação crítica prevê:
- Trilha de auditoria (ator, ação, recurso, antes/depois)
- Correlação entre requisição e log
- Idempotência quando aplicável
- Proteção contra abuso / rate limiting

### Toda integração externa prevê:
- Timeout definido
- Retry controlado com backoff
- Circuit breaker quando necessário
- Validação do payload retornado
- Credenciais em vault / variável de ambiente
- Proteção contra SSRF

---

## Estratégia de autorização

Escolher e registrar em `DECISIONS.md`:

| Modelo | Quando usar |
|---|---|
| **RBAC** | Papéis bem definidos, estáveis, sem contextualização |
| **ABAC** | Decisões dependem de ownership, tenant, status, sensibilidade |
| **Híbrido RBAC + ABAC** | Padrão recomendado para sistemas reais com área admin e regras contextuais |

---

## Observabilidade obrigatória

- Logs estruturados (JSON preferencialmente)
- Correlation ID / Trace ID propagado entre camadas
- Erros sem vazamento de stack trace para o cliente
- Eventos de auditoria separados de logs técnicos
- Nível de log configurável por ambiente

---

## Proibições

- Controller gordo (regra de negócio no controller)
- Service Deus (um service que faz tudo)
- DTO reaproveitado para entrada e saída ao mesmo tempo
- Entidade de banco exposta diretamente na interface
- Lógica de autorização espalhada em múltiplas camadas
- Segredo hardcoded em qualquer lugar
- Acoplamento da regra de negócio ao ORM
- Abstração antes de evidência real de necessidade

---

## Critérios de aceite arquitetural

A base arquitetural só é considerada correta quando:

- [ ] Árvore de diretórios foi definida e aprovada
- [ ] Camadas estão separadas e sem violação de dependência
- [ ] Contratos foram definidos antes da implementação
- [ ] Endpoints críticos têm mapeamento de ameaça
- [ ] Estratégia de autenticação e autorização está documentada em `DECISIONS.md`
- [ ] Trilha de continuidade existe em `PROJECT_STATE.md`

---

## Regra final

Quando houver escolha entre rapidez e arquitetura correta, priorizar arquitetura correta e registrar o trade-off em `DECISIONS.md`.
