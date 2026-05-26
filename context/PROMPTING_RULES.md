# PROMPTING_RULES.md
> Define como orientar agentes de código (IA) para executar trabalho técnico com controle, sem improvisar arquitetura e sem ignorar segurança.

---

## Regra base

A IA não é um gerador de código livre. Ela opera como **implementadora controlada por documentação, contratos e checkpoints**.

Qualidade do output é proporcional à qualidade da instrução e do contexto fornecido.

---

## Leitura obrigatória antes de qualquer solicitação

O agente deve ler, nesta ordem, antes de qualquer análise ou código:

1. `CONTEXT_MAP.md`
2. `PROJECT_BRIEF.md`
3. `ARCHITECTURE.md`
4. `SECURITY_RULES.md`
5. `CODING_STANDARDS.md`
6. `API_CONTRACTS.md`
7. `TASKS.md`
8. `PROJECT_STATE.md`

Se a tarefa tocar interface: também `UI_GUIDELINES.md`.

---

## O que pedir primeiro

Antes de pedir código, pedir:

1. Análise do estado atual do módulo/funcionalidade
2. Árvore / módulos afetados pela mudança
3. DTOs e contratos impactados
4. Riscos de segurança por endpoint / fluxo
5. Plano curto de implementação em etapas
6. Somente então: código

---

## O que o agente nunca deve fazer

- Começar codando sem mapear estrutura e contratos
- Alterar arquitetura sem registrar decisão em `DECISIONS.md`
- Implementar múltiplas fases ao mesmo tempo sem controle
- Ignorar autorização em qualquer operação
- Confiar em input do cliente sem validação server-side
- Adicionar dependência sem justificativa
- Concluir sessão sem atualizar checkpoint
- Responder "pronto" sem evidência mínima (build, test, log)
- Reutilizar DTO de entrada como DTO de saída
- Expor entidade de banco diretamente na interface

---

## Templates de solicitação

### Iniciar funcionalidade nova

```
Leia a documentação obrigatória.
Explique o estado atual do módulo afetado.
Mostre a árvore de diretórios/módulos que será afetada.
Defina DTOs de entrada e saída, contratos e riscos de segurança por endpoint.
Depois proponha a implementação em etapas pequenas e verificáveis.
Só então escreva o código.
Ao final, atualize TASKS.md, PROJECT_STATE.md e DECISIONS.md se necessário.
```

### Refatorar módulo existente

```
Leia a documentação obrigatória.
Explique o estado atual e o problema que justifica a refatoração.
Preserve contratos públicos e controles de segurança existentes.
Mostre a estrutura afetada e os riscos da mudança.
Implemente incrementalmente sem quebrar o restante.
Ao final, registre checkpoint e impactos nos documentos de governança.
```

### Revisão de segurança

```
Leia ARCHITECTURE.md, SECURITY_RULES.md, API_CONTRACTS.md e PROJECT_STATE.md.
Mapeie a superfície de ataque atual.
Aponte falhas por severidade: crítico, alto, médio, baixo.
Indique o que bloqueia produção.
Sugira correção em ordem de risco, sem implementar tudo de uma vez.
```

### Revisar endpoint / operação específica

```
Analise o endpoint/operação X.
Verifique: autenticação, autorização (papel + ownership), validação de entrada, rate limiting, auditoria, envelope de resposta.
Aponte o que está correto e o que está faltando.
Não reescreva o que já está correto.
```

### Encerrar sessão

```
Baseado no trabalho desta sessão:
Atualize TASKS.md com status atual de cada tarefa.
Atualize PROJECT_STATE.md com checkpoint técnico real.
Se houver nova decisão, registre em DECISIONS.md.
Aponte o próximo passo exato para a próxima sessão.
```

---

## Como a IA deve estruturar a resposta técnica

Resposta técnica ideal contém, nesta ordem:

1. **Estado atual** — o que existe hoje no ponto afetado
2. **Problema real** — o que precisa mudar e por quê
3. **Módulos / arquivos afetados**
4. **Contratos / DTOs impactados**
5. **Riscos de segurança relevantes**
6. **Plano curto** — etapas pequenas e verificáveis
7. **Implementação** — código com comentários de intenção quando necessário
8. **Validação executada** — o que foi testado ou pode ser testado
9. **Atualização de checkpoint** — o que muda nos documentos de governança

---

## Granularidade das tarefas

### Preferir
- Tarefas pequenas e verificáveis
- Entregas incrementais
- Uma área crítica por vez
- Mudanças que não quebram o que já funciona

### Evitar
- "Faça o sistema inteiro"
- "Refatore tudo"
- "Melhore a segurança geral"
- Mudanças que tocam múltiplas camadas sem controle

---

## Regra de continuidade

Ao fim de cada sessão:

- [ ] `TASKS.md` atualizado
- [ ] `PROJECT_STATE.md` atualizado
- [ ] `DECISIONS.md` atualizado (quando houver nova decisão)
- [ ] Próximo passo exato documentado

---

## Sinais de que a IA está saindo do controle

| Sinal | Ação corretiva |
|---|---|
| Código antes de estrutura | Parar. Pedir análise e árvore primeiro |
| Dependência adicionada sem justificativa | Questionar. Exigir justificativa antes de aceitar |
| Autorização ignorada | Não aceitar. Exigir controle de acesso antes |
| "Está pronto" sem evidência | Não aceitar. Exigir build, teste ou log |
| Múltiplas camadas alteradas de uma vez | Dividir. Uma camada por vez |
| DTO de entrada = DTO de saída | Corrigir. Separar explicitamente |
| Segredo no código | Bloquear. Nunca aceitar |

---

## Regra final

Em caso de dúvida, o agente para de implementar e volta à documentação.

**Velocidade sem controle gera retrabalho. Retrabalho em sistema com dados sensíveis gera risco.**
