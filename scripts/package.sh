#!/usr/bin/env bash
set -euo pipefail

# TraceGuard Audit Platform — Package Script
# Creates a clean distribution zip for Gumroad
# Usage: ./scripts/package.sh [version]

VERSION="${1:-1.0.0}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/dist"
PROJECT_NAME="traceguard-audit-platform"
ARCHIVE_NAME="${PROJECT_NAME}-v${VERSION}"

echo "==> Packaging TraceGuard Audit Platform v${VERSION}"

# Step 1: Clean build artifacts
echo "==> Cleaning build artifacts..."
cd "${PROJECT_ROOT}"

# Remove build artifacts (keep .mvn/wrapper/ for mvnw)
rm -rf backend/traceguard-api/target/
rm -rf backend/traceguard-api/.m2/
rm -rf backend/traceguard-api/.mvn-cache/
rm -f backend/traceguard-api/.mvn/wrapper/maven-wrapper.jar

# Restore maven-wrapper.jar from mvnw download
echo "==> Ensuring maven-wrapper.jar is available..."
cd backend/traceguard-api
./mvnw --version 2>/dev/null || true
cd "${PROJECT_ROOT}"

# Remove logs
find . -name "*.log" -type f -delete 2>/dev/null || true

# Step 2: Create output directory
mkdir -p "${OUTPUT_DIR}"

# Step 3: Create clean zip (exclude dev artifacts)
echo "==> Creating archive: ${OUTPUT_DIR}/${ARCHIVE_NAME}.zip"

cd "${PROJECT_ROOT}"
zip -r "${OUTPUT_DIR}/${ARCHIVE_NAME}.zip" . \
    --exclude ".git/*" \
    --exclude ".gitignore" \
    --exclude "dist/*" \
    --exclude "target/*" \
    --exclude ".m2/*" \
    --exclude ".mvn-cache/*" \
    --exclude "*.log" \
    --exclude ".DS_Store" \
    --exclude "Thumbs.db" \
    --exclude ".idea/*" \
    --exclude "*.iml" \
    --exclude ".vscode/*" \
    --exclude "**/node_modules/*" \
    --exclude "backend/infra/.env" \
    --exclude "scripts/package.sh" \
    2>&1 | tail -5

echo "==> Done!"
echo ""
echo "Package: ${OUTPUT_DIR}/${ARCHIVE_NAME}.zip"
echo "Size:    $(du -h "${OUTPUT_DIR}/${ARCHIVE_NAME}.zip" | cut -f1)"
echo ""
echo "Contents:"
unzip -l "${OUTPUT_DIR}/${ARCHIVE_NAME}.zip" | head -20
echo "  ..."
echo ""
echo "==> Ready for Gumroad upload!"
