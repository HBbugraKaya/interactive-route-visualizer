#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building React canvas..."
cd web
npm install
npm run build
cd ..

echo "Copying web/dist -> desktop/src/main/resources/web"
rm -rf desktop/src/main/resources/web
mkdir -p desktop/src/main/resources/web
cp -R web/dist/. desktop/src/main/resources/web/

echo "Done. Run: ./mvnw -pl desktop exec:java"
