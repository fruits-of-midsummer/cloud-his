#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
(cd frontend && npm ci --registry=https://registry.npmjs.org --no-audit --no-fund && npm run build)
(cd backend && mvn -gs ../.mvn/settings.xml -s ../.mvn/settings.xml -B -ntp clean verify)
printf '%s\n' '已生成 backend/target/cloud-his-1.0.0.jar'
