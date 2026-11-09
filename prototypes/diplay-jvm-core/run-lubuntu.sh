#!/usr/bin/env bash
# One-shot launcher for the wired Linux CarPlay receiver on Lubuntu. RUNTIME-UNTESTED.
# Usage: DIPLAY_DIR=~/src/DiPlay KEY_DIR=~/t7w-keys NCM_IFACE=enp0s20u1 ./run-lubuntu.sh
set -euo pipefail
: "${DIPLAY_DIR:?set DIPLAY_DIR to a DiPlay checkout}"
: "${KEY_DIR:?set KEY_DIR (must contain offline-mfi/identity.pk8 and certificate.p7b)}"
: "${NCM_IFACE:?set NCM_IFACE to the iPhone cdc_ncm netdev (see: ip link)}"
sudo systemctl stop usbmuxd 2>/dev/null || true   # usbmuxd would grab the iPhone first
cd "$(dirname "$0")"
DIPLAY_DIR="$DIPLAY_DIR" gradle --no-daemon -q installDist
exec build/install/diplay-jvm-core/bin/diplay-jvm-core \
  --key-dir "$KEY_DIR" --ncm-iface "$NCM_IFACE" "${@:-}"
