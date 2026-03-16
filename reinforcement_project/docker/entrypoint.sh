#!/bin/sh
set -eu

mkdir -p /app/.fleet_web_sessions
cd /app/frontend

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

exec npm run start -- --hostname "${HOSTNAME:-0.0.0.0}" --port "${PORT:-3000}"
