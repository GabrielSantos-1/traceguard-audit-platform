# Secrets Policy

Política de manuseio de segredos do TraceGuard Audit Platform. Vale para desenvolvimento local, CI e produção.

---

## 1. Princípios

1. **Nenhum segredo real é versionado.** Nem em código, nem em `docker-compose.yml`, nem em documentação,
   nem em mensagem de commit.
2. **Configuração por variável de ambiente.** O compose e a aplicação sempre referenciam `${VAR}`; valores
   literais só são aceitos quando são placeholders óbvios (`change_me_local_dev`).
3. **`.env.example` documenta chaves, nunca valores.** O `.env` real é ignorado pelo git.
4. **Segredo de produção nunca é reaproveitado** em desenvolvimento, teste ou CI.
5. **Rotação é procedimento normal, não emergência.** Todo segredo tem dono e caminho de troca documentado.

---

## 2. Inventário de segredos

| Segredo | Onde é usado | Requisito | Onde fica |
|---|---|---|---|
| `DB_PASSWORD` | Postgres (container `db`) e API | forte, único por ambiente | `.env` local / secret store em produção |
| `JWT_SECRET` | assinatura e validação de tokens (API) | **mínimo 32 caracteres**, aleatório | idem |
| `JWT_ISSUER` / `JWT_AUDIENCE` | validação de claims | valores por ambiente | idem |
| `REDIS_URL` | rate limiting, cache | pode conter credencial na URL | idem |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | criação do primeiro admin | **somente dev** | nunca em produção |

O arquivo `backend/infra/.env.example` é a fonte de verdade da lista de chaves. Ao adicionar uma variável
nova, atualize o exemplo **sem valor**.

---

## 3. Desenvolvimento local

```bash
cp backend/infra/.env.example backend/infra/.env
```

- Preencha `DB_PASSWORD` e `JWT_SECRET` com valores locais, descartáveis.
- O `.env` está no `.gitignore` — confirme antes de commitar: `git check-ignore -v backend/infra/.env`.
- O compose **falha de propósito** se `DB_PASSWORD` ou `JWT_SECRET` não estiverem definidos: não há fallback
  silencioso para credencial em produção.

---

## 4. CI

- O workflow `.github/workflows/traceguard-backend-ci.yml` usa valores **descartáveis** para subir os
  serviços de teste (`POSTGRES_PASSWORD`, `JWT_SECRET` de teste). Eles existem apenas durante o job.
- Segredos que precisem de proteção em pipelines devem vir de **GitHub Actions Secrets**, nunca escritos no
  YAML.
- O job `dependency-review` roda em pull requests com `fail-on-severity: high`.

---

## 5. Produção

- Injete segredos pelo mecanismo do ambiente (Docker secrets, secret manager do provedor, variáveis do
  orquestrador). Não use `.env` em disco de servidor compartilhado.
- `JWT_SECRET` de produção: gere com fonte aleatória (`openssl rand -base64 48`) e guarde no secret store.
- Nunca reutilize o `JWT_SECRET` de staging em produção — trocar o segredo invalida todos os tokens emitidos
  (comportamento desejado em caso de suspeita de vazamento).
- `SEED_ADMIN_*` deve estar **vazio** em produção; o primeiro administrador é criado por procedimento
  controlado, não por seed automático.

---

## 6. Rotação

**JWT_SECRET**

1. Gere o novo valor.
2. Atualize no secret store e reinicie a API.
3. Efeito: todos os access tokens e refresh tokens emitidos antes ficam inválidos; usuários precisam
   autenticar novamente. Comunique a janela antes de rotacionar.

**DB_PASSWORD**

1. Faça backup (ver `OPERATIONS_RUNBOOK.md`, seção 6).
2. `docker exec traceguard-db psql -U traceguard -d postgres -c "alter user traceguard with password '<nova>';"`
3. Atualize a variável no ambiente e recrie o container da API:
   `docker compose -f backend/infra/docker-compose.yml --project-name traceguard up -d api`
4. Confirme a saúde: `curl -s http://localhost:8080/actuator/health/readiness`

**Chaves de API de tenant** (gerenciadas pela própria aplicação): revogar pela área administrativa e emitir
nova — não há rotação automática.

---

## 7. Incidente histórico: senha de desenvolvimento no histórico git (RESOLVIDO)

Um commit antigo do projeto continha a **senha de desenvolvimento do PostgreSQL** escrita diretamente no
PostgreSQL no `docker-compose.yml`. Era valor de desenvolvimento local — nunca foi credencial de ambiente
exposto — mas permanecia recuperável no histórico do repositório.

**Resolvido em 18/10/2026.** O histórico foi reescrito com `git filter-repo --replace-text`, de modo que a
string não existe em nenhum commit, branch ou tag. A verificação se faz em clone novo:

```bash
git clone <url-do-repo> /tmp/verify && cd /tmp/verify
git log --all -p -S '<valor-antigo>' | wc -l    # precisa retornar 0
```

Consequências da reescrita:

- **Todos os hashes de commits anteriores mudaram.** Referências a SHAs em documentos antigos estão
  desatualizadas — vale o conteúdo, não o hash.
- Clones e forks existentes ficaram divergentes; quem tiver cópia precisa re-clonar.
- Se esse valor de senha foi usado em algum ambiente acessível de rede, **rotacione** (seção 6). Como era
  senha de desenvolvimento local, o risco prático é baixo — mas o tratamento correto é rotação.

## 8. O que nunca fazer

- Commitar `.env`, `.pem`, `.key`, `.p12` ou keystore.
- Imprimir segredo em log, mensagem de erro ou resposta de API.
- Colocar valor real como *default* de `${VAR:-...}` em compose ou YAML.
- Reaproveitar credencial de produção em ambiente de teste.
- Usar `SEED_ADMIN_*` em produção.
- Aceitar senha, token ou chave colados em issue, pull request ou conversa.

---

## 9. Verificação periódica

```bash
# nenhum arquivo sensível rastreado?
git ls-files | grep -iE '\.env$|\.pem$|\.key$|\.p12$|keystore'

# nenhum valor literal suspeito nos arquivos rastreados?
git grep -nE '(password|secret|token|api[_-]?key)\s*[:=]\s*[^$"'\''[:space:]]{8,}' -- '*.yml' '*.yaml' '*.properties' | grep -viE 'change_me|example|placeholder'

# o .env está ignorado?
git check-ignore -v backend/infra/.env
```

Rode essa verificação antes de cada publicação e a cada mudança em arquivo de configuração.
