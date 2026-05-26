# PROJECT_BRIEF.md
> Preencher no início de qualquer projeto. Governa o **porquê** e o **escopo**.

---

## Identificação do projeto

| Campo | Valor |
|---|---|
| Nome do projeto | `<PROJECT_NAME>` |
| Tipo de aplicação | `<API \| SaaS \| CRUD \| portal interno \| app mobile \| integração \| worker \| outro>` |
| Status atual | `<discovery \| bootstrap \| desenvolvimento \| hardening \| manutenção>` |
| Responsável principal | `<nome>` |
| Data de criação | `<YYYY-MM-DD>` |
| Última atualização | `<YYYY-MM-DD>` |

---

## Resumo executivo

> Descrever em até 10 linhas: o problema, a solução, o valor gerado e a criticidade do sistema.

```
<preencher>
```

---

## Problema que o sistema resolve

- Qual dor existe hoje?
- Quem sofre com essa dor?
- Por que o sistema precisa existir?
- Qual risco existe se nada for feito?

```
<preencher>
```

---

## Objetivo principal

```
<preencher — o resultado central esperado do projeto>
```

## Objetivos secundários

- `<objetivo>`
- `<objetivo>`

---

## Escopo

### Dentro do escopo (MVP)
- `<item>`
- `<item>`

### Pós-MVP (planejado)
- `<item>`
- `<item>`

### Fora do escopo
- `<item>`
- `<item>`

---

## Usuários e perfis de acesso

| Perfil | Objetivo | Dados acessados | Nível de privilégio | Risco | Observações |
|---|---|---|---|---|---|
| visitante | consultar informações públicas | dados públicos | nenhum | baixo | somente leitura |
| usuário autenticado | operar funcionalidades padrão | dados próprios | padrão | médio | acesso por ownership |
| operador interno | executar rotinas administrativas | dados da operação | elevado | alto | exigir autorização granular |
| administrador | gerenciar configurações críticas | tudo | máximo | crítico | exigir auditoria total |

> Adicionar ou remover perfis conforme o sistema real.

---

## Classificação de dados

Marcar os tipos de dados presentes no sistema:

- [ ] Dados públicos
- [ ] Dados internos
- [ ] Dados pessoais (PII)
- [ ] Dados sensíveis (saúde, financeiro, jurídico)
- [ ] Credenciais e segredos
- [ ] Segredos operacionais (API keys, tokens)
- [ ] Documentos e arquivos
- [ ] Dados financeiros
- [ ] Telemetria e logs

### Regras adicionais por classificação

| Tipo | Retenção | Mascaramento | Criptografia | Acesso | Descarte |
|---|---|---|---|---|---|
| `<tipo>` | `<regra>` | `<sim/não>` | `<em repouso / em trânsito / ambos>` | `<papéis>` | `<política>` |

---

## Requisitos funcionais

| ID | Descrição |
|---|---|
| RF-01 | `<permitir cadastro de X>` |
| RF-02 | `<registrar eventos de auditoria>` |
| RF-03 | `<listar dados com filtros e paginação>` |
| RF-04 | `<administração por papel e escopo>` |

---

## Requisitos não funcionais

| ID | Descrição |
|---|---|
| RNF-01 | Autenticação segura com expiração e rotação |
| RNF-02 | Logs estruturados com correlation ID |
| RNF-03 | Respostas de erro consistentes e sem vazar detalhes internos |
| RNF-04 | Deploy com menor privilégio |
| RNF-05 | Cobertura mínima de testes em fluxos críticos |
| RNF-06 | Nenhum segredo hardcoded |
| RNF-07 | Rate limiting em todas as superfícies expostas |
| RNF-08 | Separação de ambientes dev / staging / prod |

---

## Requisitos de segurança

| Requisito | Detalhe |
|---|---|
| Método de autenticação | `<sessão / JWT / OAuth2 / API Key / outro>` |
| Modelo de autorização | `<RBAC / ABAC / híbrido>` |
| Dados críticos | `<listar>` |
| Operações críticas | `<listar>` |
| Auditoria obrigatória | `<listar operações>` |
| Rate limiting | `<listar superfícies>` |
| Criptografia em repouso | `<sim/não — o quê>` |
| Criptografia em trânsito | `<TLS / mTLS / outro>` |
| MFA para operadores sensíveis | `<sim/não>` |
| Segregação de ambientes | `<sim/não>` |

---

## Ameaças principais esperadas

- [ ] Autenticação fraca ou ausente
- [ ] Vazamento de segredo ou credencial
- [ ] IDOR / BOLA — acesso a recurso de outro usuário
- [ ] Mass assignment — sobrescrita de campos não permitidos
- [ ] Broken Function Level Authorization (BFLA)
- [ ] Brute force em endpoints de autenticação
- [ ] Enumeração de usuários ou recursos
- [ ] Injeção (SQL, NoSQL, command, template)
- [ ] XSS via entrada de usuário
- [ ] CSRF em fluxos autenticados por cookie
- [ ] SSRF em integrações externas
- [ ] Abuso de upload (MIME falso, path traversal, tamanho excessivo)
- [ ] Dependência vulnerável na cadeia de suprimento
- [ ] Exposição de stack trace ou dados internos em erros
- [ ] Logs com dados sensíveis não mascarados
- [ ] Misconfiguration em deploy (debug ativo, CORS aberto, headers ausentes)

---

## Critérios de sucesso

- [ ] Build passa sem erros
- [ ] Testes críticos passam
- [ ] API autenticada e autorizada
- [ ] Endpoints documentados e com contrato
- [ ] Backlog da fase concluído
- [ ] Checkpoint técnico atualizado
- [ ] Pronto para deploy em ambiente controlado
- [ ] Gate de produção em `SECURITY_RULES.md` atendido

---

## Restrições

| Tipo | Detalhe |
|---|---|
| Stack obrigatória | `<preencher>` |
| Prazo | `<preencher>` |
| Equipe | `<preencher>` |
| Orçamento | `<preencher>` |
| Compliance / regulatório | `<preencher>` |
| Ambientes disponíveis | `<preencher>` |
| Integrações legadas | `<preencher>` |

---

## Premissas

- `<ex: haverá PostgreSQL disponível>`
- `<ex: identidade corporativa será definida à parte>`
- `<ex: upload não faz parte do MVP>`

---

## Regra final

Nenhuma funcionalidade deve ser implementada se não estiver alinhada com este documento ou com uma decisão registrada em `DECISIONS.md`.
