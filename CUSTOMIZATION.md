# Guia de Customização

Como renomear este template para seu próprio projeto.

---

## Índice

1. [Projeto (pom.xml)](#1-projeto-pomxml)
2. [Pacote Java (97 arquivos)](#2-pacote-java)
3. [Diretório da API](#3-diretório-da-api)
4. [Docker](#4-docker)
5. [Configurações Spring Boot](#5-configurações-spring-boot)
6. [Variáveis de ambiente](#6-variáveis-de-ambiente)
7. [CI/CD](#7-cicd)
8. [Documentação](#8-documentação)
9. [Verificação final](#9-verificação-final)

---

## 1. Projeto (pom.xml)

Arquivo: `backend/traceguard-api/pom.xml`

```xml
<groupId>com.seuprojeto</groupId>
<artifactId>seuprojeto-api</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>seuprojeto-api</name>
<description>Sua API</description>
```

---

## 2. Pacote Java

Substitua `com.traceguard` pelo seu pacote em **97 arquivos**:

```bash
# 1. Renomear diretórios
find backend/traceguard-api/src -type d -name "traceguard" -exec sh -c \
  'mv "$1" "$(echo "$1" | sed "s/traceguard/seuprojeto/")"' _ {} \;

# 2. Substituir package em todos os .java
find backend/traceguard-api/src -name "*.java" -exec sed -i \
  's/com\.traceguard/com.seuprojeto/g' {} +

# 3. Substituir package em application.yml (logging level)
sed -i 's/com\.traceguard/com.seuprojeto/g' \
  backend/traceguard-api/src/main/resources/application.yml
sed -i 's/com\.traceguard/com.seuprojeto/g' \
  backend/traceguard-api/src/main/resources/application-dev.yml
```

---

## 3. Diretório da API

```bash
mv backend/traceguard-api backend/seuprojeto-api
```

Após renomear, atualize referências ao caminho antigo:

**Docker Compose** (`backend/infra/docker-compose.yml`):
```yaml
build:
  context: ../seuprojeto-api
```

**CI/CD** (`.github/workflows/traceguard-backend-ci.yml`):
```yaml
paths:
  - 'backend/seuprojeto-api/**'
working-directory: backend/seuprojeto-api
```

---

## 4. Docker

Arquivo: `backend/infra/docker-compose.yml`

### Container names e volumes

```yaml
services:
  db:
    container_name: seuprojeto-db
    volumes:
      - seuprojeto_db_data:/var/lib/postgresql/data
  redis:
    container_name: seuprojeto-redis
    volumes:
      - seuprojeto_redis_data:/data
  api:
    container_name: seuprojeto-api

volumes:
  seuprojeto_db_data:
  seuprojeto_redis_data:
```

### Database defaults

```yaml
POSTGRES_DB: ${DB_NAME:-seuprojeto}
POSTGRES_USER: ${DB_USERNAME:-seuprojeto}
DB_URL: ${DB_URL:-jdbc:postgresql://db:5432/seuprojeto}
DB_USERNAME: ${DB_USERNAME:-seuprojeto}
```

### Dockerfile (`backend/seuprojeto-api/Dockerfile`)

```dockerfile
RUN addgroup -S seuprojeto && adduser -S seuprojeto -G seuprojeto
RUN chown -R seuprojeto:seuprojeto /app
USER seuprojeto
```

---

## 5. Configurações Spring Boot

Arquivo: `backend/seuprojeto-api/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: seuprojeto-api

logging:
  level:
    com.seuprojeto: INFO

traceguard:    # ← renomeie a seção de propriedades customizadas
  jwt:
    issuer: ${JWT_ISSUER:seuprojeto-api}
    audience: ${JWT_AUDIENCE:seuprojeto-admin-api}
```

### Profiles

**application-dev.yml**:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/seuprojeto}
    username: ${DB_USERNAME:seuprojeto}
    password: ${DB_PASSWORD:seuprojeto_dev_password}
```

**application-test.yml**:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/seuprojeto_test}
    username: ${DB_USERNAME:seuprojeto}
    password: ${DB_PASSWORD:seuprojeto_test_password}
```

**application-staging.yml** / **application-prod.yml**: atualizar referências ao nome.

---

## 6. Variáveis de ambiente

Arquivo: `backend/infra/.env.example`

```env
DB_NAME=seuprojeto
DB_URL=jdbc:postgresql://localhost:5432/seuprojeto
DB_USERNAME=seuprojeto
DB_PASSWORD=change_me_local_only

JWT_ISSUER=seuprojeto-api
JWT_AUDIENCE=seuprojeto-admin-api

SEED_ADMIN_EMAIL=admin@seuprojeto.local
```

Arquivo: `backend/seuprojeto-api/.env.example` (mesmas alterações)

---

## 7. CI/CD

Arquivo: `.github/workflows/traceguard-backend-ci.yml` (renomear o arquivo)

```bash
mv .github/workflows/traceguard-backend-ci.yml \
   .github/workflows/seuprojeto-backend-ci.yml
```

```yaml
name: seuprojeto-backend-ci

on:
  pull_request:
    paths:
      - 'backend/seuprojeto-api/**'
      - '.github/workflows/seuprojeto-backend-ci.yml'
  push:
    branches: [main]
    paths:
      - 'backend/seuprojeto-api/**'
      - '.github/workflows/seuprojeto-backend-ci.yml'

jobs:
  build-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend/seuprojeto-api

    services:
      postgres:
        env:
          POSTGRES_DB: seuprojeto
          POSTGRES_USER: seuprojeto
          POSTGRES_PASSWORD: seuprojeto_ci_password
      redis:
        image: redis:7-alpine

    env:
      DB_URL: jdbc:postgresql://localhost:5432/seuprojeto
      DB_USERNAME: seuprojeto
      DB_PASSWORD: seuprojeto_ci_password
      JWT_ISSUER: seuprojeto-api
      JWT_AUDIENCE: seuprojeto-admin-api

    steps:
      - run: test -f target/seuprojeto-api-0.0.1-SNAPSHOT.jar
```

---

## 8. Documentação

### README.md

- Título: `# SeuProjeto Audit Platform`
- Badges: manter genéricos (Java, Spring Boot, Docker — não mudam)
- Caminhos nos exemplos: `backend/seuprojeto-api/`
- Email seed: `admin@seuprojeto.local`

### context/

Substituir referências a "TraceGuard" nos documentos:

```bash
grep -rn "TraceGuard\|traceguard" context/ --include="*.md"
```

Os principais arquivos que podem conter referências:
- `context/CONTEXT_MAP.md`
- `context/ARCHITECTURE.md`
- `context/DECISIONS.md`

---

## 9. Verificação final

Após todas as alterações:

```bash
# 1. Build
cd backend/seuprojeto-api && ./mvnw -q -DskipTests package

# 2. Testes
./mvnw -q test

# 3. Verificar se não sobrou referência antiga
grep -rn "traceguard" backend/seuprojeto-api/src --include="*.java" --include="*.yml" --include="*.properties"

# 4. Subir stack
docker compose -f backend/infra/docker-compose.yml --project-name seuprojeto up -d --build

# 5. Smoke
curl http://localhost:8080/actuator/health
```

---

> **Dica:** após a customização, rode `grep -rn "traceguard\|TraceGuard" . --exclude-dir=.git` para garantir que nenhuma referência ao template original ficou para trás.
