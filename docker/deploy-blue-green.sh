#!/usr/bin/env bash
set -Eeuo pipefail

readonly BACKEND_SNIPPET="/etc/nginx/snippets/buddys-backend.conf"
readonly COMPOSE_FILE="docker-compose.yml"
readonly ENV_FILE=".env"
readonly BLUE_PORT="8081"
readonly GREEN_PORT="8082"
readonly HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
readonly HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-5}"

DOCKER_IMAGE="${1:-}"

log() {
  printf '[buddys-blue-green] %s\n' "$*"
}

fail() {
  log "ERROR: $*"
  exit 1
}

compose() {
  local project="$1"
  local port="$2"
  shift 2

  sudo env DOCKER_IMAGE="$DOCKER_IMAGE" APP_PORT="$port" \
    docker compose --env-file "$ENV_FILE" -p "$project" -f "$COMPOSE_FILE" "$@"
}

detect_current_port() {
  if [ ! -f "$BACKEND_SNIPPET" ]; then
    fail "Nginx backend snippet not found: $BACKEND_SNIPPET"
  fi

  local port
  port="$(sudo sed -nE 's/.*127\.0\.0\.1:(8081|8082).*/\1/p' "$BACKEND_SNIPPET" | tail -n 1)"

  case "$port" in
    "$BLUE_PORT"|"$GREEN_PORT")
      printf '%s\n' "$port"
      ;;
    *)
      fail "Cannot determine current backend port from $BACKEND_SNIPPET"
      ;;
  esac
}

color_for_port() {
  case "$1" in
    "$BLUE_PORT") printf 'blue\n' ;;
    "$GREEN_PORT") printf 'green\n' ;;
    *) fail "Unknown port: $1" ;;
  esac
}

project_for_color() {
  printf 'buddys-%s\n' "$1"
}

write_backend_port() {
  local port="$1"
  printf 'proxy_pass http://127.0.0.1:%s;\n' "$port" | sudo tee "$BACKEND_SNIPPET" >/dev/null
}

healthcheck() {
  local port="$1"
  local url="http://127.0.0.1:${port}/actuator/health"
  local body_file
  body_file="$(mktemp)"

  for attempt in $(seq 1 "$HEALTH_RETRIES"); do
    local status="000"
    status="$(curl -sS -o "$body_file" -w '%{http_code}' "$url" 2>/dev/null || printf '000')"

    if [ "$status" = "200" ] && grep -q '"status":"UP"' "$body_file"; then
      rm -f "$body_file"
      log "Healthcheck passed: url=$url status=$status body_status=UP"
      return 0
    fi

    log "Waiting for healthcheck: attempt=${attempt}/${HEALTH_RETRIES} url=$url status=$status"
    sleep "$HEALTH_INTERVAL_SECONDS"
  done

  log "Healthcheck failed: url=$url"
  log "Last response body:"
  sed -n '1,120p' "$body_file" || true
  rm -f "$body_file"
  return 1
}

if [ -z "$DOCKER_IMAGE" ]; then
  fail "Usage: $0 <docker-image>"
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  fail "Compose file not found: $COMPOSE_FILE"
fi

if [ ! -f "$ENV_FILE" ]; then
  fail "Environment file not found: $ENV_FILE"
fi

CURRENT_PORT="$(detect_current_port)"
CURRENT_COLOR="$(color_for_port "$CURRENT_PORT")"

if [ "$CURRENT_PORT" = "$BLUE_PORT" ]; then
  TARGET_PORT="$GREEN_PORT"
else
  TARGET_PORT="$BLUE_PORT"
fi

TARGET_COLOR="$(color_for_port "$TARGET_PORT")"
CURRENT_PROJECT="$(project_for_color "$CURRENT_COLOR")"
TARGET_PROJECT="$(project_for_color "$TARGET_COLOR")"

log "Current active color: $CURRENT_COLOR"
log "Current active port: $CURRENT_PORT"
log "Target color: $TARGET_COLOR"
log "Target port: $TARGET_PORT"
log "Docker image: $DOCKER_IMAGE"
log "Current compose project kept for rollback: $CURRENT_PROJECT"
log "Target compose project: $TARGET_PROJECT"

log "Pulling target image"
compose "$TARGET_PROJECT" "$TARGET_PORT" pull app

log "Starting target container"
compose "$TARGET_PROJECT" "$TARGET_PORT" up -d --force-recreate --remove-orphans app

if ! healthcheck "$TARGET_PORT"; then
  log "Target container logs:"
  compose "$TARGET_PROJECT" "$TARGET_PORT" logs --no-color --tail=200 app || true
  log "Stopping failed target container only"
  compose "$TARGET_PROJECT" "$TARGET_PORT" down --remove-orphans || true
  fail "Deployment failed. Nginx remains on ${CURRENT_COLOR}:${CURRENT_PORT}"
fi

log "Switching Nginx backend to ${TARGET_COLOR}:${TARGET_PORT}"
write_backend_port "$TARGET_PORT"

if ! sudo nginx -t; then
  log "Nginx config test failed. Restoring backend to ${CURRENT_COLOR}:${CURRENT_PORT}"
  write_backend_port "$CURRENT_PORT"
  sudo nginx -t || true
  fail "Deployment failed during nginx -t. Previous backend restored."
fi

if ! sudo systemctl reload nginx; then
  log "Nginx reload failed. Restoring backend to ${CURRENT_COLOR}:${CURRENT_PORT}"
  write_backend_port "$CURRENT_PORT"
  sudo nginx -t || true
  sudo systemctl reload nginx || true
  fail "Deployment failed during nginx reload. Previous backend restored."
fi

log "Deployment succeeded"
log "Active color: $TARGET_COLOR"
log "Active port: $TARGET_PORT"
log "Previous container kept for rollback: ${CURRENT_COLOR}:${CURRENT_PORT}"
