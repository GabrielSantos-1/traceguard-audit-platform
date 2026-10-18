#!/usr/bin/env bash
# Builds the React dashboard and copies it into the Spring Boot static resources.
#
# Why this exists: the product ships as ONE artifact. `java -jar traceguard-api.jar`
# must serve both the API and the dashboard, so the compiled frontend has to live
# inside the jar. This script produces that state; run it whenever the frontend changes.
#
# Usage: ./scripts/build-frontend.sh
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="${PROJECT_ROOT}/frontend"
STATIC_DIR="${PROJECT_ROOT}/backend/traceguard-api/src/main/resources/static"

echo "==> Building the dashboard (Vite + TypeScript)"
cd "${FRONTEND_DIR}"
if [ ! -d node_modules ]; then
  echo "==> Installing dependencies (first run only)"
  npm install --no-audit --no-fund
fi
npm run build

echo "==> Copying the build into the backend static resources"
rm -rf "${STATIC_DIR}/assets"
mkdir -p "${STATIC_DIR}"
cp -R "${FRONTEND_DIR}/dist/." "${STATIC_DIR}/"

echo "==> Frontend in place:"
find "${STATIC_DIR}" -type f | sed "s|${PROJECT_ROOT}/||" | sed 's/^/    /'

echo
echo "==> Next step: rebuild the jar so it picks the dashboard up"
echo "    cd backend/traceguard-api && ./mvnw -DskipTests package"
echo "    java -jar target/traceguard-api-*.jar --spring.profiles.active=h2"
