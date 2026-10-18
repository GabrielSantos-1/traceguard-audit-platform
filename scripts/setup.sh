#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
API_DIR="$PROJECT_ROOT/backend/traceguard-api"
INFRA_DIR="$PROJECT_ROOT/backend/infra"

echo "=== TraceGuard Setup ==="

if ! command -v java &>/dev/null; then
  echo "ERROR: Java 21+ required. Install Temurin JDK 21."
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '\d+' | head -1)
if [ "$JAVA_VER" -lt 21 ]; then
  echo "ERROR: Java 21+ required. Found version: $(java -version 2>&1 | head -1)"
  exit 1
fi
echo "[OK] Java version: $(java -version 2>&1 | head -1)"

if ! command -v docker &>/dev/null; then
  echo "ERROR: Docker required."
  exit 1
fi
echo "[OK] Docker: $(docker --version)"

if ! docker compose version &>/dev/null; then
  echo "ERROR: Docker Compose plugin required."
  exit 1
fi
echo "[OK] Docker Compose: $(docker compose version)"

if [ ! -f "$INFRA_DIR/.env" ]; then
  cp "$INFRA_DIR/.env.example" "$INFRA_DIR/.env"
  echo "[OK] .env criado a partir de .env.example"
  echo "     Edite backend/infra/.env para alterar credenciais padrao."
else
  echo "[OK] .env ja existe"
fi

echo ""
echo "=== Build da API ==="
cd "$API_DIR"
./mvnw -q -DskipTests -Dmaven.repo.local=.m2/repository package
cd "$PROJECT_ROOT"
echo "[OK] Build concluido"

echo ""
echo "=== Subindo stack (Postgres + Redis + API) ==="
docker compose -f "$INFRA_DIR/docker-compose.yml" --project-name traceguard up -d --build

echo ""
echo "=== Aguardando healthcheck ==="
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/actuator/health/readiness > /dev/null 2>&1; then
    echo "[OK] API pronta em http://localhost:8080"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERROR: API nao ficou pronta apos 30s. Verifique logs com:"
    echo "  docker compose -f $INFRA_DIR/docker-compose.yml logs api"
    exit 1
  fi
  sleep 2
done

echo ""
echo "=== Setup concluido ==="
echo ""
echo "Credenciais padrao (dev):"
echo "  Email:    admin@traceguard.local"
echo "  Password: admin123"
echo ""
echo "Login:"
echo "  curl -X POST http://localhost:8080/api/v1/auth/login \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"email\":\"admin@traceguard.local\",\"password\":\"admin123\"}'"
echo ""
echo "Para desligar:"
echo "  docker compose -f $INFRA_DIR/docker-compose.yml down"
