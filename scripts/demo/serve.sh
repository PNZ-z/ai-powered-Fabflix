#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-4173}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# macOS's Python may be blocked by the local-network privacy prompt; WEBrick
# (Ruby, shipped with macOS) binds reliably. Keep the script dependency-free.
if command -v ruby >/dev/null 2>&1; then
  echo "Serving ${ROOT}/demo on http://0.0.0.0:${PORT}/ (ruby/WEBrick)"
  exec ruby -run -e httpd "${ROOT}/demo" -p "${PORT}"
fi

echo "Serving ${ROOT}/demo on http://0.0.0.0:${PORT}/ (python fallback)"
exec python3 -m http.server "${PORT}" --bind 0.0.0.0 --directory "${ROOT}/demo"
