# Laptop CarPlay: Bliss OS + stock DiPlay APK (2026-11-09)

Jonah's choice (2026-11-09): forget the T7W and the MFi chip for now and get CarPlay running on a laptop with the identity bundled in DiPlay's release APK. This **overrides D8 for personal laptop testing**. The identity stays out of this repo, because the repo is public.

**Why this route:** the release APK already contains the identity and native libraries for x86/x86_64 (DiPlay docs/VALIDATION.md, CHANGELOG #613). A Bliss OS (Android-x86) user has already run DiPlay (DiPlay release notes 0.2.20, #609). Booting Android on the laptop gives DiPlay a real USB host, so no new code is needed. **Not verified by us:** nobody here has run this combination.

## Steps
1. **Make a Bliss OS USB stick.** Download an x86_64 Android 13/14 ISO (any Android 10+ build works for DiPlay, and Android 10+ is needed for its verified Wi-Fi Direct path). Write it to a USB stick with Ventoy or `dd`. Boot the laptop from it in **live mode**, so nothing is installed and the CachyOS disk isn't touched. Secure Boot may need to be off.
2. **Get the APK.** Download `DiPlay-0.2.22.apk` from the official release (github.com/shihabal3amri/DiPlay/releases/tag/v0.2.22) and check its SHA-256 against the published checksum. Install it from the file manager, or run `adb install -r DiPlay-0.2.22.apk` over Wi-Fi ADB.
3. **Connect wired first.** It's the simplest path. Use a data cable on a USB-A/C **data** port. In DiPlay tap **Connect with USB**, then approve USB access, Trust on the iPhone, and the local VPN prompt. That VPN carries the USB network link and is not an internet VPN.
4. **Start with low settings:** 30 fps, HEVC off, and 80% resolution if the video stutters.
5. **Wireless (optional, later):** use **Existing Wi-Fi / Same LAN** (laptop and iPhone on the same router). The laptop's Wi-Fi chip may not support Wi-Fi Direct under Android-x86.

## Known risks / unknowns
- **USB:** some laptops' USB controllers behave oddly under Android-x86. If **Connect with USB** never sees the iPhone, try another port, preferably USB-A.
- **Identity:** it's Carlinkit's Apple-issued accessory identity, and Apple can revoke it in any iOS update. If CarPlay stops authenticating after an iOS update, that's the likely cause.
- **Graphics/audio:** Android-x86 driver support varies by laptop. Intel iGPUs are usually the safest bet.
- If a report is needed: **Settings → Diagnostics → Save diagnostic report** (it saves to Downloads/DiPlay).

## Fallback: native Linux
Under CachyOS itself, the work left is in [diplay-audit.md](diplay-audit.md) §4: libusb iPhone host, media sink, controller. The key-file client (`mfi/LocalMfiAuthenticationClient`) is already part of the compiled core in `prototypes/diplay-jvm-core`. It would load `offline-mfi/identity.pk8` + `certificate.p7b` from a gitignored local folder, never from the repo.
