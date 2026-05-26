# CODING_STANDARDS.md
> Padrões de código para legibilidade, consistência, segurança e manutenção. Não são sugestões — são o padrão do projeto.

---

## Princípios

| # | Princípio |
|---|---|
| 1 | Código legível é melhor do que código "esperto" |
| 2 | Cada arquivo tem responsabilidade única e clara |
| 3 | Segurança deve ser visível no código, não implícita |
| 4 | Nomear por intenção, não por conveniência |
| 5 | Abstração entra quando há ganho real e evidência |
| 6 | TODO sem contexto é lixo técnico |
| 7 | Duplicação relevante deve ser removida |
| 8 | Funções longas e arquivos gigantes devem ser quebrados |

---

## Tipagem e linguagem

- TypeScript / linguagem fortemente tipada é o padrão quando disponível
- `strict: true` habilitado sempre que a stack permitir
- Tipos explícitos em boundaries críticas (entradas de API, DTOs, eventos)
- Evitar `any`, casts cegos e `@ts-ignore` sem justificativa documentada
- Generics quando há ganho real de tipagem — não por padrão

---

## Ordem de implementação

Para cada funcionalidade, seguir esta ordem sem exceção:

```
1. Módulo / arquivo afetado na árvore
2. DTO de entrada e DTO de saída
3. Validações e regras de negócio
4. Política de autorização
5. Caso de uso (orquestração)
6. Persistência / integração
7. Controller / handler / interface
8. Testes
9. Documentação e checkpoint
```

---

## Convenções de nomenclatura

### Arquivos e diretórios

| Contexto | Convenção | Exemplo |
|---|---|---|
| Diretórios | `kebab-case` | `use-cases/`, `value-objects/` |
| Classes / componentes | `PascalCase` | `UserService`, `AuthController` |
| Funções / variáveis | `camelCase` | `getUserById`, `isExpired` |
| Constantes globais | `UPPER_SNAKE_CASE` | `MAX_RETRY_ATTEMPTS` |
| Arquivos de classe | `PascalCase.ts` | `UserRepository.ts` |
| Arquivos de função / util | `kebab-case.ts` | `hash-password.ts` |

### Tipos e interfaces

| Tipo | Sufixo | Exemplo |
|---|---|---|
| DTO de entrada | `CreateDTO`, `UpdateDTO` | `CreateUserDTO` |
| DTO de saída | `ResponseDTO` | `UserResponseDTO` |
| Comando | `Command` | `RegisterUserCommand` |
| Query | `Query` | `FindUserQuery` |
| Política / Guard | `Policy` | `AdminOnlyPolicy` |
| Repositório | `Repository` | `UserRepository` |
| Schema de validação | `Schema` | `CreateUserSchema` |
| Evento | `Event` | `UserRegisteredEvent` |
| Erro customizado | `Error` | `UnauthorizedError` |

---

## Responsabilidade por camada

### Interface (controller / handler)
```
✓ Parsear entrada e converter para DTO
✓ Chamar caso de uso
✓ Normalizar resposta com envelope padrão
✗ Conter regra de negócio crítica
✗ Acessar banco diretamente
✗ Tomar decisão de autorização complexa
```

### Aplicação (caso de uso)
```
✓ Orquestrar fluxo do caso de uso
✓ Verificar autorização explícita
✓ Chamar portas / repositórios / services
✗ Conhecer detalhes de framework HTTP
✗ Importar ORM diretamente
```

### Domínio
```
✓ Conter regra de negócio pura
✓ Impor invariantes e impedir estados inválidos
✗ Importar framework, ORM, HTTP ou UI
✗ Conhecer detalhes de persistência
```

### Infraestrutura
```
✓ Implementar contratos definidos pela aplicação
✓ Lidar com ORM, cache, filas, SDKs externos
✗ Conter regra de negócio
✗ Ser chamada diretamente pelo domínio
```

---

## Comentários

Comentários devem explicar:
- **Intenção** — por que a decisão foi tomada
- **Trade-off** — o que foi sacrificado
- **Risco** — o que pode falhar
- **Limitação** — o que não foi implementado e por quê

### Não usar comentários para:
- Explicar o óbvio (`// incrementa o contador`)
- Substituir código legível
- Deixar código morto comentado no repositório

### TODOs
```typescript
// TODO(gabriel, 2025-01-15): substituir por query parametrizada quando migrar para v2
// Motivo: endpoint legado; workaround temporário aceito em DECISIONS-007
```

Sem contexto e responsável — TODO não entra.

---

## Tratamento de erros

### Regras

- Nunca engolir exceção silenciosamente
- Nunca retornar erro bruto de provider para o cliente
- Usar tipos de erro normalizados por categoria

### Hierarquia de erros customizados

```typescript
// Base
class AppError extends Error {
  constructor(
    public readonly code: string,
    public readonly message: string,
    public readonly statusCode: number,
    public readonly correlationId?: string
  ) { super(message); }
}

// Especializados
class ValidationError extends AppError { /* 400 */ }
class UnauthorizedError extends AppError { /* 401 */ }
class ForbiddenError extends AppError { /* 403 */ }
class NotFoundError extends AppError { /* 404 */ }
class ConflictError extends AppError { /* 409 */ }
class RateLimitError extends AppError { /* 429 */ }
class DependencyError extends AppError { /* 502 */ }
```

### Middleware de erro (exemplo)

```typescript
// Captura centralizada — stack trace nunca chega ao cliente
app.use((error, req, res, next) => {
  const correlationId = req.correlationId ?? generateCorrelationId();
  
  if (error instanceof AppError) {
    logger.warn({ correlationId, code: error.code, message: error.message });
    return res.status(error.statusCode).json({
      success: false,
      error: { code: error.code, message: error.message, correlationId }
    });
  }

  logger.error({ correlationId, error: error.message, stack: error.stack });
  return res.status(500).json({
    success: false,
    error: { code: 'INTERNAL_ERROR', message: 'Erro interno.', correlationId }
  });
});
```

---

## Dependências

Checklist antes de adicionar:

- [ ] O problema pode ser resolvido com recurso nativo?
- [ ] A biblioteca é madura, ativa e mantida?
- [ ] Há CVEs conhecidos? (`npm audit`, `snyk`, `trivy`)
- [ ] O bundle / footprint compensa para o uso previsto?
- [ ] A licença é compatível (MIT, Apache 2.0, etc.)?
- [ ] A dependência tem lockfile?

Dependência sem justificativa em `DECISIONS.md` não entra.

---

## Formulários e entrada de dados

```
✓ Validar no client para UX responsiva
✓ Validar novamente no server — sempre, obrigatoriamente
✗ Confiar em campos ocultos
✗ Deixar frontend decidir privilégio
✗ Aceitar objeto livre sem schema
✗ Passar input diretamente para ORM (mass assignment)
```

---

## Banco e persistência

- Usar migrations versionadas — nunca alterar schema manualmente em prod
- Evitar query raw sem justificativa forte documentada
- Repositório reflete intenção de negócio (`findActiveUsersByTenant`, não `select * where active=1`)
- Nunca vazar entidade de persistência para interface sem passar por `ResponseDTO`
- Toda alteração crítica é auditável (before/after no log de auditoria)
- Índices planejados junto com queries — não como afterthought

---

## Testes

### Mínimo esperado por funcionalidade relevante

| Tipo | Cobre | Quando |
|---|---|---|
| Unitário | regra de negócio, validação, policy | sempre que houver lógica |
| Integração | fluxo com banco, cache, serviço externo | sempre que houver persistência ou I/O |
| Autorização | acesso negado + acesso permitido por papel | sempre em operação com authz |
| Regressão | bug corrigido | a cada correção |

### O que testar obrigatoriamente

- [ ] Validação de schema (campos inválidos rejeitados)
- [ ] Autorização (acesso negado para papel incorreto)
- [ ] Acesso negado por ownership (IDOR)
- [ ] Caso de uso crítico ponta a ponta
- [ ] Resposta de erro consistente (sem dado interno)
- [ ] Comportamento idempotente quando declarado

---

## Observabilidade no código

```typescript
// Correlation ID propagado do middleware para todo o fluxo
// Nunca logar segredo, token ou PII
// Mensagens úteis para investigação — evitar "erro genérico"
logger.info({
  correlationId: ctx.correlationId,
  action: 'USER_CREATED',
  userId: user.id,
  tenantId: user.tenantId,
});
```

---

## Commits

Padrão obrigatório (Conventional Commits):

| Prefixo | Quando usar |
|---|---|
| `feat:` | nova funcionalidade |
| `fix:` | correção de bug |
| `refactor:` | refatoração sem mudança de comportamento |
| `docs:` | documentação |
| `test:` | adição ou correção de testes |
| `chore:` | tarefa de manutenção (deps, build, config) |
| `security:` | correção ou melhoria de segurança |
| `perf:` | melhoria de performance |

Evitar: `"ajustes"`, `"update"`, `"misc"`, `"wip"` sem contexto.

---

## Checklist de encerramento de tarefa

Antes de considerar uma tarefa concluída:

- [ ] Contrato respeitado (DTO, envelope, erro)
- [ ] Autorização aplicada e testada
- [ ] Input validado server-side
- [ ] Erro normalizado (sem stack trace, sem dado interno)
- [ ] Testes relevantes implementados e passando
- [ ] Nenhum segredo no código
- [ ] Nenhuma dependência adicionada sem justificativa
- [ ] Checkpoint atualizado (`PROJECT_STATE.md`, `TASKS.md`)

---

## Regra final

Priorizar sempre, nesta ordem:

```
1. Segurança
2. Legibilidade
3. Consistência
4. Manutenção futura
5. Baixo acoplamento
```

Performance e "elegância" vêm depois dessas cinco.
