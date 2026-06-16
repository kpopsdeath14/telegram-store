#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: ./setup_project.sh <project_name> <catalog_bot_name> <catalog_bot_token> <admin_bot_name> <admin_bot_token>

Example:
  ./setup_project.sh jemo_murge catalog_bot 123:ABC admin_bot 456:DEF
EOF
}

if [ $# -ne 5 ]; then
  usage
  exit 1
fi

PROJECT_NAME="$1"
CATALOG_BOT_NAME="$2"
CATALOG_BOT_TOKEN="$3"
ADMIN_BOT_NAME="$4"
ADMIN_BOT_TOKEN="$5"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DEPLOY_ROOT="${DEPLOY_ROOT:-/home/timofey}"
SYSTEMD_DIR="${SYSTEMD_DIR:-/etc/systemd/system}"
ECOSYSTEM_CONFIG="${ECOSYSTEM_CONFIG:-/home/timofey/ecosystem.config.js}"
ANGIE_ROUTES_DIR="${ANGIE_ROUTES_DIR:-/etc/angie/routes.d/tg-market}"

PROJECT_SLUG="${PROJECT_NAME//_/-}"
CATALOG_FRONT_NAME="${PROJECT_NAME}-frontend"
CATALOG_BACK_NAME="${PROJECT_NAME}-backend"
ADMIN_FRONT_NAME="${PROJECT_NAME}-admin-frontend"
ADMIN_BACK_NAME="${PROJECT_NAME}-admin-backend"
ADMIN_SLUG="${PROJECT_SLUG}-admin"
DB_DATE="$(date +%Y_%m_%d)"
DB_NAME="${DB_DATE}_BIBIZEN_${CATALOG_BOT_NAME}"

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

PM2_CATALOG_NAME="$PROJECT_NAME"
PM2_ADMIN_NAME="${PROJECT_NAME}_admin"

PORT_BASE="$(python3 - <<'PY'
import socket

def port_free(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.bind(("127.0.0.1", port))
    except OSError:
        return False
    finally:
        sock.close()
    return True

for n in range(0, 500):
    base = 3200 + 100 * n
    ports = [base, base + 1, base + 100, base + 101]
    if all(port_free(p) for p in ports):
        print(base)
        raise SystemExit(0)

raise SystemExit("No free port block found starting at 3200 with step 100")
PY
)"

CATALOG_FRONT_PORT="$PORT_BASE"
CATALOG_BACK_PORT="$((PORT_BASE + 1))"
ADMIN_FRONT_PORT="$((PORT_BASE + 100))"
ADMIN_BACK_PORT="$((PORT_BASE + 101))"

echo "==> Port block selected"
echo "    catalog front: $CATALOG_FRONT_PORT"
echo "    catalog back:  $CATALOG_BACK_PORT"
echo "    admin front:   $ADMIN_FRONT_PORT"
echo "    admin back:    $ADMIN_BACK_PORT"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
}

require_cmd npm
require_cmd npx
require_cmd java
require_cmd curl
require_cmd python3

ensure_lein() {
  if command -v lein >/dev/null 2>&1; then
    return 0
  fi

  echo "Leiningen not found. Attempting to install..."

  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y leiningen
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y leiningen
  elif command -v yum >/dev/null 2>&1; then
    sudo yum install -y leiningen
  elif command -v pacman >/dev/null 2>&1; then
    sudo pacman -Sy --noconfirm leiningen
  else
    echo "No supported package manager found. Installing via script..."
    sudo mkdir -p /usr/local/bin
    sudo curl -fsSL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein
    sudo chmod +x /usr/local/bin/lein
  fi

  if ! command -v lein >/dev/null 2>&1; then
    echo "Failed to install leiningen. Install it manually and re-run." >&2
    exit 1
  fi
}

ensure_lein

echo "==> Compile templates"
compile_once() {
  local dir="$1"
  local name="$2"
  local artifact="$3"
  if [ -f "$artifact" ]; then
    echo "  - skip $name (artifact exists)"
    return 0
  fi
  (cd "$dir" && bash compilate_from_template.sh "$name")
}

compile_once "$CATALOG_FRONT_DIR" "$CATALOG_FRONT_NAME" "$CATALOG_FRONT_BUILD/public/js/app.js"
compile_once "$CATALOG_BACK_DIR" "$CATALOG_BACK_NAME" "$CATALOG_BACK_BUILD/target/backend.jar"
compile_once "$ADMIN_FRONT_DIR" "$ADMIN_FRONT_NAME" "$ADMIN_FRONT_BUILD/public/js/app.js"
compile_once "$ADMIN_BACK_DIR" "$ADMIN_BACK_NAME" "$ADMIN_BACK_BUILD/target/backend.jar"

echo "==> Prepare deploy directories"
if [ -d "$CATALOG_DIR" ] || [ -d "$ADMIN_DIR" ]; then
  echo "  - deploy dirs already exist, skipping creation"
else
  mkdir -p "$CATALOG_DIR" "$ADMIN_DIR"
fi

echo "==> Copy catalog frontend artifacts"
cp "$CATALOG_FRONT_BUILD/public/js/app.js" "$CATALOG_DIR/app.js"
cp "$CATALOG_FRONT_BUILD/public/index.html" "$CATALOG_DIR/index.html"
cp "$CATALOG_FRONT_BUILD/public/css/site.css" "$CATALOG_DIR/site.css"
cp "$CATALOG_FRONT_BUILD/public/config.edn" "$CATALOG_DIR/config.edn"
cp "$CATALOG_FRONT_BUILD/public/share.svg" "$CATALOG_DIR/share.svg"
cp "$CATALOG_FRONT_BUILD/public/server.js" "$CATALOG_DIR/server.js"
cp "$CATALOG_FRONT_BUILD/package.json" "$CATALOG_DIR/package.json"

echo "==> Copy catalog backend artifacts"
cp "$CATALOG_BACK_BUILD/target/backend.jar" "$CATALOG_DIR/backend.jar"
cp "$CATALOG_BACK_BUILD/app_state.edn" "$CATALOG_DIR/app_state.edn"

echo "==> Copy admin frontend artifacts"
cp "$ADMIN_FRONT_BUILD/public/js/app.js" "$ADMIN_DIR/app.js"
cp "$ADMIN_FRONT_BUILD/public/index.html" "$ADMIN_DIR/index.html"
cp "$ADMIN_FRONT_BUILD/public/css/site.css" "$ADMIN_DIR/site.css"
cp "$ADMIN_FRONT_BUILD/public/config.edn" "$ADMIN_DIR/config.edn"
cp "$ADMIN_FRONT_BUILD/public/server.js" "$ADMIN_DIR/server.js"
cp "$ADMIN_FRONT_BUILD/package.json" "$ADMIN_DIR/package.json"

echo "==> Copy admin backend artifacts"
cp "$ADMIN_BACK_BUILD/target/backend.jar" "$ADMIN_DIR/backend.jar"
cp "$ADMIN_BACK_BUILD/app_state.edn" "$ADMIN_DIR/app_state.edn"

mkdir -p "$ADMIN_DIR/images" "$ADMIN_DIR/banners"

echo "==> Patch index.html paths"
python3 - <<PY
from pathlib import Path

def clean_index(path):
    p = Path(path)
    text = p.read_text()
    lines = []
    for line in text.splitlines():
        if "react-phone-number-input/style.css" in line:
            continue
        if "<base href=" in line:
            continue
        lines.append(line)
    p.write_text("\\n".join(lines) + "\\n")

clean_index("$CATALOG_DIR/index.html")
clean_index("$ADMIN_DIR/index.html")
PY

echo "==> Patch frontend server ports"
python3 - <<PY
from pathlib import Path
def set_port(path, port):
    p = Path(path)
    text = p.read_text()
    text = text.replace("const port = 3700;", f"const port = {port};")
    p.write_text(text)
set_port("$CATALOG_DIR/server.js", $CATALOG_FRONT_PORT)
set_port("$ADMIN_DIR/server.js", $ADMIN_FRONT_PORT)
PY

echo "==> Install runtime dependencies (express only)"
install_express() {
  local dir="$1"
  if [ -d "$dir/node_modules/express" ]; then
    echo "  - express already installed in $dir"
    return 0
  fi
  (cd "$dir" && npm install --omit=dev --no-save express)
}

install_express "$CATALOG_DIR"
install_express "$ADMIN_DIR"

echo "==> Update config.edn and app_state.edn"
python3 - <<PY
import re
from pathlib import Path

def replace_edn(path, mapping):
    p = Path(path)
    text = p.read_text()
    for key, value in mapping.items():
        pattern_quoted = r"(:%s\\s+\")([^\"]*)(\")" % re.escape(key)
        new_text, n = re.subn(
            pattern_quoted,
            lambda m, v=value: f"{m.group(1)}{v}{m.group(3)}",
            text,
            count=1,
        )
        if n == 0:
            pattern_unquoted = r"(:%s\\s+)([^\\s\\n\\r;]+)" % re.escape(key)
            new_text, n = re.subn(
                pattern_unquoted,
                lambda m, v=value: f"{m.group(1)}{v}",
                text,
                count=1,
            )
        if n == 0:
            raise SystemExit(f"Key not found in {path}: {key}")
        text = new_text
    p.write_text(text)

replace_edn(
    "$CATALOG_DIR/config.edn",
    {
        "backend_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-api/api/",
        "admin_frontend_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin/",
        "bot_name": "$CATALOG_BOT_NAME",
    },
)

replace_edn(
    "$ADMIN_DIR/config.edn",
    {
        "backend_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin-api/api/",
        "frontend_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin/",
        "admin_bot_name": "$ADMIN_BOT_NAME",
        "project-name": "$PROJECT_SLUG",
    },
)

replace_edn(
    "$CATALOG_DIR/app_state.edn",
    {
        "bot-token": "$CATALOG_BOT_TOKEN",
        "bot-admin-token": "$ADMIN_BOT_TOKEN",
        "dbname": "$DB_NAME",
        "admin_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin",
    },
)

replace_edn(
    "$ADMIN_DIR/app_state.edn",
    {
        "bot-token": "$ADMIN_BOT_TOKEN",
        "catalog-bot-token": "$CATALOG_BOT_TOKEN",
        "dbname": "$DB_NAME",
        "admin_url": "https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin",
    },
)

def replace_unquoted_port(path, value):
    p = Path(path)
    text = p.read_text()
    new_text, n = re.subn(
        r"(^\\s*:port\\s+)(\\d+)(\\b)",
        lambda m, v=value: f"{m.group(1)}{v}{m.group(3)}",
        text,
        count=1,
        flags=re.M,
    )
    if n == 0:
        raise SystemExit(f"Unquoted :port not found in {path}")
    p.write_text(new_text)

replace_unquoted_port("$CATALOG_DIR/app_state.edn", "$CATALOG_BACK_PORT")
replace_unquoted_port("$ADMIN_DIR/app_state.edn", "$ADMIN_BACK_PORT")
PY

echo "==> Create systemd services"
CATALOG_SERVICE="${PROJECT_NAME}_backend"
ADMIN_SERVICE="${PROJECT_NAME}_admin_backend"

sudo tee "$SYSTEMD_DIR/${CATALOG_SERVICE}.service" >/dev/null <<EOF
[Unit]
Description=${PROJECT_NAME} backend
Wants=basic.target
After=basic.target network.target

[Service]
User=timofey
WorkingDirectory=${CATALOG_DIR}
ExecStart=/usr/bin/java -jar ${CATALOG_DIR}/backend.jar
Type=simple
SuccessExitStatus=143
TimeoutStopSec=10
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo tee "$SYSTEMD_DIR/${ADMIN_SERVICE}.service" >/dev/null <<EOF
[Unit]
Description=${PROJECT_NAME} admin backend
Wants=basic.target
After=basic.target network.target

[Service]
User=timofey
WorkingDirectory=${ADMIN_DIR}
ExecStart=/usr/bin/java -jar ${ADMIN_DIR}/backend.jar
Type=simple
SuccessExitStatus=143
TimeoutStopSec=10
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo service "$CATALOG_SERVICE" start
sudo service "$ADMIN_SERVICE" start

echo "==> Update PM2 ecosystem"
python3 - <<PY
from pathlib import Path
import re

path = Path("$ECOSYSTEM_CONFIG")
text = path.read_text()

entries = []

def has(name):
    return (f"name: '{name}'" in text) or (f'name: \"{name}\"' in text)

if not has("$PM2_CATALOG_NAME"):
    entries.append(
        "  {\\n"
        "    name: '${PM2_CATALOG_NAME}',\\n"
        "    cwd: '${CATALOG_DIR}',\\n"
        "    script: 'server.js',\\n"
        "    watch: false\\n"
        "  },"
    )

if not has("$PM2_ADMIN_NAME"):
    entries.append(
        "  {\\n"
        "    name: '${PM2_ADMIN_NAME}',\\n"
        "    cwd: '${ADMIN_DIR}',\\n"
        "    script: 'server.js',\\n"
        "    watch: false\\n"
        "  },"
    )

if entries:
    idx = text.rfind("]")
    if idx == -1:
        raise SystemExit("Invalid ecosystem.config.js: missing closing ]")
    before = text[:idx].rstrip()
    after = text[idx:]
    if re.search(r"\\{\\s*name\\s*:", before):
        if not before.endswith(","):
            before = before + ","
    before = before + "\\n" + "\\n".join(entries) + "\\n"
    path.write_text(before + after)
PY

pm2 start "$ECOSYSTEM_CONFIG" --only "$PM2_CATALOG_NAME"
pm2 start "$ECOSYSTEM_CONFIG" --only "$PM2_ADMIN_NAME"

echo "==> Configure Telegram bots"
make_request() {
  local token="$1"
  local method="$2"
  local endpoint="$3"
  local data="$4"

  response=$(curl -s -w "\n%{http_code}" -X "$method" \
    -H "Content-Type: application/json" \
    -d "$data" \
    "https://api.telegram.org/bot${token}${endpoint}")

  http_code=$(echo "$response" | tail -n1)
  response_body=$(echo "$response" | sed '$d')

  if [ "$http_code" -ne 200 ]; then
    echo "Telegram API error: HTTP $http_code"
    echo "Response: $response_body"
    return 1
  fi

  echo "$response_body"
  return 0
}

set_webhook() {
  local token="$1"
  local webhook_url="$2"

  response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "{\"url\":\"$webhook_url\"}" \
    "https://api.telegram.org/bot${token}/setWebhook")

  http_code=$(echo "$response" | tail -n1)
  response_body=$(echo "$response" | sed '$d')

  if [ "$http_code" -ne 200 ]; then
    echo "Telegram API error: HTTP $http_code"
    echo "Response: $response_body"
    return 1
  fi

  echo "$response_body"
  return 0
}

setup_catalog_bot() {
  local bot_name="$1"
  local bot_token="$2"
  local webapp_url="https://tg-market.qq-pp.ru/${PROJECT_SLUG}/"
  local webhook_url="https://tg-market.qq-pp.ru/${PROJECT_SLUG}-api/webhook/telegram"

  local commands='{"commands":[{"command":"start","description":"Start bot"}],"scope":{"type":"default"},"language_code":"ru"}'
  make_request "$bot_token" "POST" "/setMyCommands" "$commands" >/dev/null

  local menu_button="{\"menu_button\":{\"type\":\"web_app\",\"text\":\"Open Catalog\",\"web_app\":{\"url\":\"$webapp_url\"}}}"
  make_request "$bot_token" "POST" "/setChatMenuButton" "$menu_button" >/dev/null

  set_webhook "$bot_token" "$webhook_url" >/dev/null

  echo "Catalog bot configured: $bot_name"
}

setup_admin_bot() {
  local bot_name="$1"
  local bot_token="$2"
  local webapp_url="https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin/"
  local webhook_url="https://tg-market.qq-pp.ru/${PROJECT_SLUG}-admin-api/webhook/telegram"

  local commands='{"commands":[{"command":"start","description":"Start bot"},{"command":"request","description":"Leave a request"}],"scope":{"type":"default"},"language_code":"ru"}'
  make_request "$bot_token" "POST" "/setMyCommands" "$commands" >/dev/null

  local menu_button="{\"menu_button\":{\"type\":\"web_app\",\"text\":\"Open App\",\"web_app\":{\"url\":\"$webapp_url\"}}}"
  make_request "$bot_token" "POST" "/setChatMenuButton" "$menu_button" >/dev/null

  set_webhook "$bot_token" "$webhook_url" >/dev/null

  echo "Admin bot configured: $bot_name"
  echo "IMPORTANT: set login domain in BotFather for admin bot:"
  echo "  /setdomain -> ${bot_name} -> tg-market.qq-pp.ru"
}

echo "==> Add Angie route"
if [ ! -d "$ANGIE_ROUTES_DIR" ]; then
  echo "Angie routes dir not found: $ANGIE_ROUTES_DIR" >&2
  exit 1
fi

ANGIE_EXISTING=$(ls "$ANGIE_ROUTES_DIR"/*-"$PROJECT_SLUG".conf 2>/dev/null | head -n1 || true)
ANGIE_EXISTS=0

if [ -n "$ANGIE_EXISTING" ]; then
  ANGIE_PATH="$ANGIE_EXISTING"
  ANGIE_EXISTS=1
  echo "Angie config already exists: $ANGIE_PATH"
else
  next_num=0
  if ls "$ANGIE_ROUTES_DIR"/*.conf >/dev/null 2>&1; then
    last_num=$(ls "$ANGIE_ROUTES_DIR"/*.conf | sed -E 's|.*/([0-9]+)-.*|\\1|' | sort -n | tail -1)
    if [[ "$last_num" =~ ^[0-9]+$ ]]; then
      last_num=$((10#$last_num))
      next_num=$((last_num + 1))
    fi
  fi
  ANGIE_FILE="$(printf "%02d-%s.conf" "$next_num" "$PROJECT_SLUG")"
  ANGIE_PATH="$ANGIE_ROUTES_DIR/$ANGIE_FILE"
fi

if [ "$ANGIE_EXISTS" -eq 0 ]; then
  sudo tee "$ANGIE_PATH" >/dev/null <<EOF
# ${PROJECT_SLUG}
# ===============

location ^~ /${PROJECT_SLUG}/ {
    proxy_pass http://127.0.0.1:${CATALOG_FRONT_PORT}/;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
}

location ^~ /${PROJECT_SLUG}-api/ {
    proxy_pass http://127.0.0.1:${CATALOG_BACK_PORT}/;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
}

location ^~ /${PROJECT_SLUG}-admin/ {
    proxy_pass http://127.0.0.1:${ADMIN_FRONT_PORT}/;
    proxy_set_header Host \$host;

    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;

    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
}

location ^~ /${PROJECT_SLUG}-admin-api/ {
    client_max_body_size 100M;

    proxy_pass http://127.0.0.1:${ADMIN_BACK_PORT}/;
    proxy_set_header Host \$host;

    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;

    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
}
EOF
fi

sudo service angie restart

setup_catalog_bot "$CATALOG_BOT_NAME" "$CATALOG_BOT_TOKEN"
setup_admin_bot "$ADMIN_BOT_NAME" "$ADMIN_BOT_TOKEN"

echo "==> Done"
echo "Project dir: $PROJECT_DIR"
