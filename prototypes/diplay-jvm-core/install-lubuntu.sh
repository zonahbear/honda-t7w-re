#!/usr/bin/env bash
# One-shot installer for the wired Linux CarPlay receiver on Lubuntu/Debian-based systems.
# Installs deps, fetches the DiPlay release APK + its bundled identity, clones DiPlay's source,
# installs the udev rule and builds the launcher. It does NOT connect to the iPhone — run it once,
# then follow the printed command with your iPhone plugged in.
#
#   bash install-lubuntu.sh
#
# Env overrides: DIPLAY_TAG (default v0.2.22), DIPLAY_SRC (~/src/DiPlay), KEY_DIR (~/t7w-keys).
# The extracted identity is Carlinkit's borrowed Apple credential; keep KEY_DIR off any public repo.
set -euo pipefail

DIPLAY_TAG="${DIPLAY_TAG:-v0.2.22}"
DIPLAY_SRC="${DIPLAY_SRC:-$HOME/src/DiPlay}"
KEY_DIR="${KEY_DIR:-$HOME/t7w-keys}"
REPO="shihabal3amri/DiPlay"
SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

say() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

command -v apt-get >/dev/null || { echo "This installer targets Debian/Ubuntu (Lubuntu). No apt-get found."; exit 1; }

say "Installing packages (needs sudo)"
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk ffmpeg libusb-1.0-0 usbmuxd curl jq unzip git

say "Fetching DiPlay $DIPLAY_TAG APK (holds the bundled identity)"
work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
apk_url="$(curl -fsSL "https://api.github.com/repos/$REPO/releases/tags/$DIPLAY_TAG" \
  | jq -r '.assets[] | select(.name|endswith(".apk")) | .browser_download_url' | head -n1)"
[ -n "$apk_url" ] || { echo "Could not find a .apk asset on release $DIPLAY_TAG"; exit 1; }
curl -fsSL "$apk_url" -o "$work/DiPlay.apk"

say "Extracting the offline-mfi identity to $KEY_DIR/offline-mfi"
mkdir -p "$KEY_DIR/offline-mfi"
unzip -o -j "$work/DiPlay.apk" 'assets/offline-mfi/identity.pk8' 'assets/offline-mfi/certificate.p7b' -d "$KEY_DIR/offline-mfi"
chmod 600 "$KEY_DIR/offline-mfi/"* || true
[ -s "$KEY_DIR/offline-mfi/identity.pk8" ] && [ -s "$KEY_DIR/offline-mfi/certificate.p7b" ] \
  || { echo "The APK did not contain assets/offline-mfi/{identity.pk8,certificate.p7b}."; exit 1; }

say "Cloning DiPlay source ($DIPLAY_TAG) to $DIPLAY_SRC"
if [ -d "$DIPLAY_SRC/.git" ]; then git -C "$DIPLAY_SRC" fetch --depth 1 origin "$DIPLAY_TAG" && git -C "$DIPLAY_SRC" checkout -q FETCH_HEAD
else git clone --depth 1 --branch "$DIPLAY_TAG" "https://github.com/$REPO" "$DIPLAY_SRC"; fi

say "Installing udev rule for iPhone USB access"
sudo cp "$SELF_DIR/70-t7w-iphone.rules" /etc/udev/rules.d/
sudo udevadm control --reload

say "Building the launcher"
( cd "$SELF_DIR" && DIPLAY_DIR="$DIPLAY_SRC" ./gradlew --no-daemon -q installDist )

cat <<DONE

Done. To run it:
  1. Plug in and unlock the iPhone, then find its network interface:
       ip link        # look for a new 'enp0s...u...' entry that appears on plug-in
  2. Start the receiver (replace the interface name):
       sudo systemctl stop usbmuxd
       DIPLAY_DIR="$DIPLAY_SRC" KEY_DIR="$KEY_DIR" NCM_IFACE=enp0s20u1 \\
         "$SELF_DIR/run-lubuntu.sh"
  Tap Trust on the iPhone when asked. The CarPlay bring-up is still untested, so capture the log.
DONE
