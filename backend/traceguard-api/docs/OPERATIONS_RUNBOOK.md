# Operations Runbook

Procedimentos operacionais do TraceGuard Audit Platform (API + PostgreSQL + Redis via Docker Compose).

Escopo: subir, verificar saúde, fazer backup, atualizar, reverter e diagnosticar. Os comandos abaixo
referem-se ao arquivo `backend/infra/docker-compose.yml` e ao projeto Compose `traceguard`.

---

## 1. Pré-requisitos

| Item | Requisito | Como validar |
|---|---|---|
| Java | 21+ | `java -version` |
| Docker | com plugin Compose v2 | `docker compose version` |
| Portas livres | 5432 (db), 6379 (redis), 8080 (api) | `ss -ltnp \| grep -E ':(5432\|6379\|8080)'` |

O script `scripts/setup.sh` valida Java, Docker e o plugin Compose automaticamente e aborta se faltar algo.

> **Atenção:** se você já roda outro PostgreSQL ou Redis na máquina (por exemplo, um Supabase local), as
> portas 5432/6379 podem estar ocupadas. Nesse caso, ajuste o mapeamento de portas no compose ou pare o
> serviço concorrente antes de subir o stack.

---

## 2. Primeira execução

```bash
# 1. variáveis de ambiente (nunca commitar o .env)
cp backend/infra/.env.example backend/infra/.env
#    edite o .env e preencha DB_PASSWORD e JWT_SECRET (mínimo 32 caracteres)

# 2. build do jar (o Dockerfile também compila, mas fazer local acelera o ciclo)
cd backend/traceguard-api && ./mvnw -q -DskipTests package && cd ../..

# 3. subir o stack
docker compose -f backend/infra/docker-compose.yml --project-name traceguard up -d --build
```

O serviço `api` só inicia depois que `db` e `redis` passam no healthcheck (`depends_on: condition: service_healthy`).
As migrations Flyway rodam automaticamente na inicialização da aplicação.

---

## 3. Verificação de saúde

```bash
# API (readiness — o mesmo endpoint usado pelo healthcheck do container)
curl -s http://localhost:8080/actuator/health/readiness

# Containers e estado
docker compose -f backend/infra/docker-compose.yml --project-name traceguard ps

# Dependências isoladamente
docker exec traceguard-db pg_isready -U traceguard -d traceguard
docker exec traceguard-redis redis-cli ping
```

Swagger UI para teste manual de endpoints: `http://localhost:8080/swagger-ui.html`.

---

## 4. Logs

```bash
# API em tempo real
docker compose -f backend/infra/docker-compose.yml --project-name traceguard logs -f api

# últimas 200 linhas de um serviço específico
docker logs --tail 200 traceguard-api
docker logs --tail 100 traceguard-db
```

Cada requisição carrega um **correlation ID**; use-o para correlacionar entradas no log da API.

---

## 5. Migrations (Flyway)

As migrations ficam em `backend/traceguard-api/src/main/resources/db/migration/` (V1–V14) e são aplicadas
automaticamente na subida da aplicação.

```bash
# histórico aplicado
docker exec traceguard-db psql -U traceguard -d traceguard -c "select version, description, success, installed_on from flyway_schema_history order by installed_rank;"
```

**Regras:**

- Migrations são **forward-only**: não há scripts de *undo*. Reverter schema exige restaurar backup ou
  escrever uma migration compensatória.
- **Sempre faça backup antes de subir uma versão com migration nova.**
- Nunca edite uma migration já aplicada em ambiente com dados: crie uma nova versão.

---

## 6. Backup

```bash
# dump lógico (recomendado antes de cada deploy)
docker exec traceguard-db pg_dump -U traceguard -d traceguard --format=custom \
  > traceguard-$(date +%Y%m%d-%H%M).dump

# apenas o schema
docker exec traceguard-db pg_dump -U traceguard -d traceguard --schema-only > traceguard-schema.sql
```

Os dados ficam nos volumes `traceguard_db_data` e `traceguard_redis_data`. Backup de volume é alternativa
ao dump lógico, não substituto: prefira o dump para portabilidade.

---

## 7. Restauração

```bash
# 1. parar a API para evitar escrita durante a restauração
docker compose -f backend/infra/docker-compose.yml --project-name traceguard stop api

# 2. recriar o banco e restaurar
docker exec -i traceguard-db psql -U traceguard -d postgres -c "drop database traceguard with (force);"
docker exec -i traceguard-db psql -U traceguard -d postgres -c "create database traceguard owner traceguard;"
docker exec -i traceguard-db pg_restore -U traceguard -d traceguard --no-owner < traceguard-AAAAMMDD-HHMM.dump

# 3. subir novamente
docker compose -f backend/infra/docker-compose.yml --project-name traceguard start api
```

Teste a restauração periodicamente em ambiente descartável — backup não testado não é backup.

---

## 8. Atualização (deploy)

```bash
git pull

# backup antes de qualquer migration nova (seção 6)
docker exec traceguard-db pg_dump -U traceguard -d traceguard --format=custom > pre-deploy.dump

# rebuild e recriação apenas do que mudou
docker compose -f backend/infra/docker-compose.yml --project-name traceguard up -d --build

# confirmar
curl -s http://localhost:8080/actuator/health/readiness
```

---

## 9. Rollback

| Cenário | Procedimento |
|---|---|
| Falha de aplicação (sem migration nova) | `git checkout <commit-ou-tag-anterior>` → `docker compose ... up -d --build` |
| Falha após migration nova | Restaurar o backup pré-deploy (seção 7) e voltar o código ao commit anterior |
| Migration aplicada com sucesso mas feature com defeito | Desabilitar a feature ou escrever migration compensatória; **não** apagar a linha de `flyway_schema_history` |

---

## 10. Diagnóstico rápido

| Sintoma | Verificar |
|---|---|
| Container `api` reiniciando em loop | `docker logs traceguard-api` — normalmente `DB_PASSWORD`/`JWT_SECRET` ausente ou banco indisponível |
| `api` não sobe | `docker compose ps` — confirme que `db` e `redis` estão `healthy` |
| 401 em todas as chamadas | `JWT_SECRET` diferente entre quem emite e quem valida; confira o valor efetivo no ambiente do container |
| Rate limit bloqueando indevidamente | `RATE_LIMIT_REDIS_ENABLED` e disponibilidade do Redis (sem Redis o limitador usa fallback in-memory, que não é compartilhado entre instâncias) |
| Porta 8080 em uso | outro processo na porta: `ss -ltnp \| grep :8080` |

---

## 11. Referência de variáveis

| Variável | Obrigatória | Observação |
|---|---|---|
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | sim | usadas pelo container `db` e pela API |
| `DB_URL` | sim (prod) | padrão `jdbc:postgresql://db:5432/traceguard` |
| `REDIS_URL` | sim | padrão `redis://redis:6379` |
| `JWT_SECRET` | sim | mínimo 32 caracteres; único por ambiente |
| `RATE_LIMIT_REDIS_ENABLED` | não | padrão `true` |
| `SPRING_PROFILES_ACTIVE` | não | padrão `prod` no compose; perfis disponíveis: `dev`, `h2`, `test`, `staging`, `prod` |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | só em dev | criam o primeiro administrador na inicialização |

Detalhes de manuseio de segredos: ver `SECRETS_POLICY.md`. Antes de expor a produção: `PRODUCTION_READINESS_CHECKLIST.md`.
