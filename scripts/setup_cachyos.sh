#!/usr/bin/env bash
# Set up the offline firmware-analysis environment on CachyOS / Arch Linux.
#
#   scripts/setup_cachyos.sh            # install what is missing, create .venv
#   scripts/setup_cachyos.sh --check    # report only, change nothing
#
# Packages are checked against the repos with `pacman -Si` before install, so a
# renamed or AUR-only package is reported instead of failing the whole run.
# Python deps go into ./.venv, never system-wide.
set -euo pipefail

CHECK_ONLY=0
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=1

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Core: inspection, unpacking and filesystems seen on Android/Linux head units.
PKGS=(
  git python python-pip file binutils    # file, strings, readelf, objdump
  binwalk                                # signature scan / carving
  unzip p7zip xz lz4 zstd lzop cpio gzip bzip2
  squashfs-tools e2fsprogs mtd-utils     # unsquashfs, debugfs, ubi tools
  android-tools                          # adb, fastboot, simg2img (if present)
  hexyl                                  # readable hex dumps
  usbutils                               # lsusb, for dongle/USB experiments
  jdk21-openjdk ghidra                   # disassembly / decompilation
  jadx apktool                           # APK / dex inspection (may be AUR)
)

if ! command -v pacman >/dev/null; then
  echo "pacman not found: this script targets CachyOS/Arch." >&2
  exit 1
fi

missing=() unavailable=()
for p in "${PKGS[@]}"; do
  pacman -Qi "$p" >/dev/null 2>&1 && continue
  if pacman -Si "$p" >/dev/null 2>&1; then missing+=("$p"); else unavailable+=("$p"); fi
done

echo "Installed already: $(( ${#PKGS[@]} - ${#missing[@]} - ${#unavailable[@]} ))/${#PKGS[@]}"
[[ ${#missing[@]} -gt 0 ]] && echo "To install from repos: ${missing[*]}"
[[ ${#unavailable[@]} -gt 0 ]] && echo "Not in enabled repos (try AUR, e.g. paru -S): ${unavailable[*]}"

if [[ $CHECK_ONLY -eq 1 ]]; then
  [[ -d "$REPO_ROOT/.venv" ]] && echo ".venv present" || echo ".venv missing"
  exit 0
fi

if [[ ${#missing[@]} -gt 0 ]]; then
  sudo pacman -S --needed --noconfirm "${missing[@]}"
fi

# Python tools in a project-local venv.
if [[ ! -d "$REPO_ROOT/.venv" ]]; then
  python -m venv "$REPO_ROOT/.venv"
fi
"$REPO_ROOT/.venv/bin/pip" install --quiet --upgrade pip
"$REPO_ROOT/.venv/bin/pip" install --quiet -r "$REPO_ROOT/requirements.txt"

echo
echo "Versions:"
for c in file binwalk readelf unsquashfs simg2img ghidraRun jadx apktool; do
  if command -v "$c" >/dev/null; then
    printf '  %-11s %s\n' "$c" "$("$c" --version 2>&1 | head -n1 || true)"
  else
    printf '  %-11s MISSING\n' "$c"
  fi
done
echo
echo "Self-test:"
"$REPO_ROOT/.venv/bin/python" -m unittest discover -s "$REPO_ROOT/tests"
