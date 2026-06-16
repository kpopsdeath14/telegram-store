#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./recompile_project.sh <project_name>

Rebuilds artifacts from templates and copies:
  - app.js (catalog + admin frontends)
  - backend.jar (catalog + admin backends)

Optional env vars:
  DEPLOY_ROOT=/home/timofey          # where projects are deployed
EOF
}

if [ $# -ne 1 ]; then
  usage
  exit 1
fi

PROJECT_NAME="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DEPLOY_ROOT="${DEPLOY_ROOT:-/home/timofey}"
RESTART_SYSTEMD=1
RESTART_PM2=1

CATALOG_FRONT_NAME="${PROJECT_NAME}-frontend"
CATALOG_BACK_NAME="${PROJECT_NAME}-backend"
ADMIN_FRONT_NAME="${PROJECT_NAME}-admin-frontend"
ADMIN_BACK_NAME="${PROJECT_NAME}-admin-backend"

CATALOG_FRONT_DIR="$SCRIPT_DIR/telegram-store-frontend"
CATALOG_BACK_DIR="$SCRIPT_DIR/telegram-store-backend"
ADMIN_FRONT_DIR="$SCRIPT_DIR/admin-telegram-frontend"
ADMIN_BACK_DIR="$SCRIPT_DIR/admin-telegram-backend"

CATALOG_FRONT_BUILD="$CATALOG_FRONT_DIR/$CATALOG_FRONT_NAME"
CATALOG_BACK_BUILD="$CATALOG_BACK_DIR/$CATALOG_BACK_NAME"
ADMIN_FRONT_BUILD="$ADMIN_FRONT_DIR/$ADMIN_FRONT_NAME"
ADMIN_BACK_BUILD="$ADMIN_BACK_DIR/$ADMIN_BACK_NAME"

PROJECT_DIR="$DEPLOY_ROOT/$PROJECT_NAME"
CATALOG_DIR="$PROJECT_DIR/catalog"
ADMIN_DIR="$PROJECT_DIR/admin"

if [ ! -d "$CATALOG_DIR" ] || [ ! -d "$ADMIN_DIR" ]; then
  echo "Deploy dirs not found:"
  echo "  $CATALOG_DIR"
  echo "  $ADMIN_DIR"
  exit 1
fi

compile_once() {
  local dir="$1"
  local name="$2"
  (cd "$dir" && bash compilate_from_template.sh "$name")
}

echo "==> Recompile templates"
compile_once "$CATALOG_FRONT_DIR" "$CATALOG_FRONT_NAME"
compile_once "$CATALOG_BACK_DIR" "$CATALOG_BACK_NAME"
compile_once "$ADMIN_FRONT_DIR" "$ADMIN_FRONT_NAME"
compile_once "$ADMIN_BACK_DIR" "$ADMIN_BACK_NAME"

echo "==> Copy frontend app.js"
cp "$CATALOG_FRONT_BUILD/public/js/app.js" "$CATALOG_DIR/app.js"
cp "$ADMIN_FRONT_BUILD/public/js/app.js" "$ADMIN_DIR/app.js"

echo "==> Copy backend jars"
cp "$CATALOG_BACK_BUILD/target/backend.jar" "$CATALOG_DIR/backend.jar"
cp "$ADMIN_BACK_BUILD/target/backend.jar" "$ADMIN_DIR/backend.jar"

if [ "$RESTART_SYSTEMD" = "1" ]; then
  echo "==> Restart systemd services"
  sudo service "${PROJECT_NAME}_backend" restart
  sudo service "${PROJECT_NAME}_admin_backend" restart
fi

if [ "$RESTART_PM2" = "1" ]; then
  echo "==> Restart PM2 apps"
  pm2 restart "${PROJECT_NAME}"
  pm2 restart "${PROJECT_NAME}_admin"
fi

echo "Done."
