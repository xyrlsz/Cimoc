#!/usr/bin/env bash
#
# data_server native-only build script.
# - Builds ONLY for the current host (GOOS/GOARCH detected via `go env`).
# - CGO is always enabled (gorm.io/driver/sqlite requires mattn/go-sqlite3 via CGO).
#
# Usage:
#   ./build.sh                     # native build -> ./dist/
#   ./build.sh -out ./artifacts   # custom output directory
#   ./build.sh -n                 # dry-run: print commands only
#   ./build.sh -v                 # verbose (adds -x to go build)
#   ./build.sh -h                 # help
#
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

OUT_DIR="$SCRIPT_DIR/dist"
VERBOSE=0
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage: ./build.sh [options]

Builds data_server for the CURRENT host platform only (native build).
Uses CGO unconditionally because gorm.io/driver/sqlite is backed by
mattn/go-sqlite3, which requires a local C compiler.

Options:
  -out <dir>     Output directory (default: ./dist)
  -n | -dry-run  Print commands, do not execute
  -v             Verbose (passes -x to go build)
  -h | --help    Show this help

Prerequisites:
  * Go >= 1.20
  * A local C compiler (gcc / clang) in PATH:
      - Debian/Ubuntu:  sudo apt-get install gcc
      - macOS:          xcode-select --install
      - WSL:            sudo apt-get install gcc
      - Windows MSYS2:  pacman -S --needed mingw-w64-ucrt-x86_64-gcc
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -out)        OUT_DIR="$2"; shift 2 ;;
    -n|-dry-run) DRY_RUN=1; shift ;;
    -v)          VERBOSE=1; shift ;;
    -h|--help)   usage; exit 0 ;;
    *)           echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

# ---- helpers ---------------------------------------------------------
run() {
  if [[ $DRY_RUN -eq 1 ]]; then
    echo "RUN: $*"
  else
    echo "RUN: $*"
    "$@"
  fi
}

require_cmd() {
  local cmd="$1"; local hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: missing required command: $cmd" >&2
    [[ -n "${hint:-}" ]] && echo "       hint: $hint" >&2
    exit 2
  fi
}

# ---- main ------------------------------------------------------------
require_cmd go "Install Go >=1.20: https://go.dev/dl/"
require_cmd gcc "Install a host C compiler (gcc/clang) for CGO."

mkdir -p "$OUT_DIR"

BUILD_FLAGS=(-ldflags "-s -w")
if [[ $VERBOSE -eq 1 ]]; then BUILD_FLAGS+=(-x); fi

host_os="$(go env GOOS)"
host_arch="$(go env GOARCH)"
ext=""
[[ "$host_os" == "windows" ]] && ext=".exe"
OUT_FILE="$OUT_DIR/xcimoc-data-server-${host_os}-${host_arch}${ext}"

echo "==> Build native ($host_os/$host_arch)"
CGO_ENABLED=1 run go build "${BUILD_FLAGS[@]}" -o "$OUT_FILE" .

if [[ $DRY_RUN -eq 0 ]]; then
  cp -f "$SCRIPT_DIR/config.example.yaml" "$OUT_DIR/"
  echo ""
  echo "Build succeeded. Binaries in: $OUT_DIR"
  ls -lh "$OUT_DIR"
fi
