#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEEP_STACK_RUNNING="${KEEP_STACK_RUNNING:-0}"
STACK_WAS_ALREADY_RUNNING=0

containers=(
  "smartairbase-postgres"
  "smartairbase-mcpserver"
  "smartairbase-mcpclient"
  "smartairbase-frontend"
)

cleanup() {
  if [[ "$STACK_WAS_ALREADY_RUNNING" == "1" || "$KEEP_STACK_RUNNING" == "1" ]]; then
    return
  fi

  echo
  echo "Stopping Smart Air Base docker stack..."
  (cd "$ROOT_DIR" && docker compose down >/dev/null)
}

fail() {
  echo
  echo "Smoke test failed: $1" >&2
  echo
  echo "Recent compose logs:" >&2
  (cd "$ROOT_DIR" && docker compose logs --no-color --tail 120) >&2 || true
  exit 1
}

trap cleanup EXIT

get_container_status() {
  local container_name="$1"
  docker inspect "$container_name" \
    --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' 2>/dev/null || true
}

get_container_state() {
  local container_name="$1"
  docker inspect "$container_name" --format '{{.State.Status}}' 2>/dev/null || true
}

wait_for_container() {
  local container_name="$1"
  local expected_status="$2"
  local timeout_seconds="${3:-120}"
  local waited=0

  echo "Waiting for ${container_name} to become ${expected_status}..."

  while (( waited < timeout_seconds )); do
    local status
    local state

    status="$(get_container_status "$container_name")"
    state="$(get_container_state "$container_name")"

    if [[ "$status" == "$expected_status" ]]; then
      echo "${container_name} is ${expected_status}."
      return 0
    fi

    if [[ "$state" == "exited" || "$state" == "dead" ]]; then
      fail "${container_name} entered state ${state}"
    fi

    sleep 2
    waited=$((waited + 2))
  done

  fail "Timed out waiting for ${container_name} to become ${expected_status}"
}

assert_http_200() {
  local url="$1"
  local body_file
  local http_code

  body_file="$(mktemp)"
  http_code="$(curl --silent --show-error --location --output "$body_file" --write-out '%{http_code}' "$url")" || {
    rm -f "$body_file"
    fail "Request to ${url} failed"
  }

  if [[ "$http_code" != "200" ]]; then
    echo "Response body from ${url}:" >&2
    cat "$body_file" >&2
    rm -f "$body_file"
    fail "Expected HTTP 200 from ${url}, got ${http_code}"
  fi

  rm -f "$body_file"
  echo "HTTP 200 OK: ${url}"
}

wait_for_http_200() {
  local url="$1"
  local timeout_seconds="${2:-60}"
  local waited=0

  echo "Waiting for HTTP 200 from ${url}..."

  while (( waited < timeout_seconds )); do
    if curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' "$url" | grep -qx '200'; then
      echo "HTTP 200 OK: ${url}"
      return 0
    fi

    sleep 2
    waited=$((waited + 2))
  done

  fail "Timed out waiting for HTTP 200 from ${url}"
}

create_and_abort_game() {
  local create_response
  local game_id
  local abort_code

  create_response="$(curl --silent --show-error \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{}' \
    http://localhost:3000/api/scenarios/1/create-game)" || fail "Could not create a smoke-test game"

  game_id="$(printf '%s' "$create_response" | sed -n 's/.*"gameId":[[:space:]]*\([0-9][0-9]*\).*/\1/p')"

  if [[ -z "$game_id" ]]; then
    echo "Create-game response:" >&2
    printf '%s\n' "$create_response" >&2
    fail "Could not extract gameId from create-game response"
  fi

  echo "Created smoke-test game ${game_id}."

  abort_code="$(curl --silent --show-error \
    --output /dev/null \
    --write-out '%{http_code}' \
    -X POST \
    "http://localhost:3000/api/games/${game_id}/abort")" || fail "Could not abort smoke-test game ${game_id}"

  if [[ "$abort_code" != "200" ]]; then
    fail "Expected HTTP 200 while aborting game ${game_id}, got ${abort_code}"
  fi

  echo "Aborted smoke-test game ${game_id}."
}

cd "$ROOT_DIR"

if docker ps --format '{{.Names}}' | grep -qx 'smartairbase-frontend'; then
  STACK_WAS_ALREADY_RUNNING=1
fi

echo "Building and starting Smart Air Base docker stack..."
docker compose up -d --build

wait_for_container "smartairbase-postgres" "healthy" 120
wait_for_container "smartairbase-mcpserver" "healthy" 180
wait_for_container "smartairbase-mcpclient" "healthy" 180

wait_for_http_200 "http://localhost:3000/" 60
wait_for_http_200 "http://localhost:3000/api/reference/rules" 60
wait_for_http_200 "http://localhost:8080/api/reference/rules" 60

create_and_abort_game

echo
echo "Docker smoke test passed."

if [[ "$STACK_WAS_ALREADY_RUNNING" == "1" ]]; then
  echo "The stack was already running, so it has been left up."
elif [[ "$KEEP_STACK_RUNNING" == "1" ]]; then
  echo "KEEP_STACK_RUNNING=1, leaving the stack up."
fi
