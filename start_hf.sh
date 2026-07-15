#!/usr/bin/env bash
set -euo pipefail

export PORT="${PORT:-7860}"
export LINJIAN_PORT="${LINJIAN_PORT:-8513}"
export LINJIAN_HOST="${LINJIAN_HOST:-0.0.0.0}"
export LINJIAN_URL="${LINJIAN_URL:-http://127.0.0.1:${LINJIAN_PORT}}"
export LINJIAN_INTERNAL_URL="${LINJIAN_INTERNAL_URL:-http://127.0.0.1:${LINJIAN_PORT}}"
export LINJIAN_PROXY_TARGET="${LINJIAN_PROXY_TARGET:-${LINJIAN_INTERNAL_URL}}"
export LINJIAN_DATA_DIR="${LINJIAN_DATA_DIR:-/home/node/app/data}"
export LINJIAN_DEFAULT_DEVICE="${LINJIAN_DEFAULT_DEVICE:-ios-phone}"
export LINJIAN_KEEP="${LINJIAN_KEEP:-3}"
export LINJIAN_HF_PROXY="${LINJIAN_HF_PROXY:-1}"

mkdir -p "$LINJIAN_DATA_DIR"

if [ -z "${LINJIAN_TOKEN:-}" ]; then
  echo "[掌心窗 iOS Lite] WARNING: LINJIAN_TOKEN is empty. Set it as a Hugging Face Space Secret before using Shortcuts or MCP tools." >&2
fi

echo "[掌心窗 iOS Lite] starting Python backend on ${LINJIAN_HOST}:${LINJIAN_PORT}"
PORT="$LINJIAN_PORT" python3 server/linjian_server.py &
PY_PID=$!

sleep 1

echo "[掌心窗 iOS Lite] starting Node MCP/public proxy on 0.0.0.0:${PORT}"
cd mcp
node server.js &
NODE_PID=$!
cd ..

cleanup() {
  echo "[掌心窗 iOS Lite] stopping..."
  kill "$PY_PID" "$NODE_PID" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

wait -n "$PY_PID" "$NODE_PID"
