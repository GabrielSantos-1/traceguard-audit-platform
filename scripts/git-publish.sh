#!/usr/bin/env bash
set -euo pipefail

# ==============================================================
# git-publish.sh — Organiza staging, commita e sobe pro GitHub
# Uso: bash scripts/git-publish.sh
# ==============================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  TraceGuard — Git Publish Script${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# 1. Verificar se está no branch correto
BRANCH=$(git branch --show-current)
if [ "$BRANCH" != "master" ]; then
    echo -e "${YELLOW}Aviso: você está no branch '$BRANCH', não em 'master'${NC}"
    read -rp "Continuar mesmo assim? (y/N) " confirm
    if [ "$confirm" != "y" ]; then
        echo -e "${RED}Abortado.${NC}"
        exit 1
    fi
fi

# 2. Mostrar status atual
echo -e "${YELLOW}Status atual do repositório:${NC}"
git status --short
echo ""

# 3. Limpar staging e readicionar tudo
echo -e "${YELLOW}Passo 1/4 — Resetando staging e readicionando com .gitignore atualizado...${NC}"
git reset HEAD -- . > /dev/null 2>&1 || true
git add .
echo -e "${GREEN}  ✓ Staging atualizado${NC}"
echo ""

# 4. Mostrar o que vai no commit
echo -e "${YELLOW}Passo 2/4 — Arquivos que serão commitados:${NC}"
git status --short
echo ""

# 5. Commit
echo -e "${YELLOW}Passo 3/4 — Criando commit...${NC}"
git commit -m "feat: complete TraceGuard security audit platform

Full implementation including:
- JWT auth with refresh token rotation
- RBAC (SUPER_ADMIN, TENANT_ADMIN, OPERATOR, AUDITOR)
- Multi-tenancy with data isolation
- API key authentication (scopes AUDIT_READ, AUDIT_WRITE)
- Dual audit trails (admin actions + business events)
- Rate limiting (in-memory + Redis + resilient)
- Correlation ID tracing
- Dashboard with KPIs and timelines
- Integration configs
- OpenAPI/Swagger documentation
- 78 passing tests
- 13 governance documents
- Docker Compose (PostgreSQL + Redis)
- Frontend SPA inline"
echo -e "${GREEN}  ✓ Commit criado${NC}"
echo ""

# 6. Push
echo -e "${YELLOW}Passo 4/4 — Enviando para GitHub...${NC}"
git push origin "$BRANCH"
echo -e "${GREEN}  ✓ Push realizado${NC}"
echo ""

# 7. Resumo
echo -e "${CYAN}========================================${NC}"
echo -e "${GREEN}  Publicação concluída!${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo -e "Repositório: ${CYAN}https://github.com/GabrielSantos-1/traceguard-audit-platform${NC}"
echo ""
echo -e "${YELLOW}Nota: O commit '30172ed' ainda contém '[senha-de-dev-removida]' no histórico.${NC}"
echo -e "${YELLOW}Antes de tornar o repositório público, rode:${NC}"
echo -e "${CYAN}  git filter-repo --force --replace-text <(echo '[senha-de-dev-removida]') --refs HEAD${NC}"
echo ""
