# Breadcrumbs (short-term memory, newest first)

## 2026-11-09 (later): native Lubuntu receiver
- Jonah: dropped the T7W/MFi-chip goal, wants it on a laptop with DiPlay's bundled key, target **Lubuntu**. Overrides D8 for laptop use; never commit the key (repo is public).
- Built prototypes/diplay-jvm-core app (com.shilapi.xcertplay.desktop): LinuxIphoneUsb (usb4java; vendor 0x52 switch + USBMUX claim), FfmpegMediaSink (ffplay out), DesktopReceiver (= Android CarPlayController.runStack on Linux; kernel cdc_ncm replaces userspace NCM/VpnService), Main CLI. Deps added: usb4java 1.3.0, kxml2. installDist builds; --help runs.
- 294 DiPlay files now portable (added IphoneUsbHost seam). RUNTIME-UNTESTED: no iPhone/key in sandbox. TOUCH NOT WIRED (ffplay is a separate window; needs an in-process surface). Mic/sync/HEVC also pending.
- docs/laptop-carplay-native-linux.md: APK key-extraction (apktool) + Lubuntu run steps. udev rule (70-t7w-iphone.rules) + run-lubuntu.sh in the prototypes dir.

## 2026-11-09: DiPlay audit thread (branch claude/project-thread-t0dqd3)
- **Did:** audited github.com/shihabal3amri/DiPlay @615b32c → docs/diplay-audit.md. Added tools/port_scope.py + tests/test_port_scope.py (16 tests OK). Added prototypes/diplay-jvm-core (276 DiPlay files compile on JDK 21 / Kotlin 2.2.10 with our stubs in shim/ and seams/; nothing executed).
- **Key facts:** DiPlay = xcertplay fork, GPL-3.0, Android ≥7, a full iAP2 + AirPlay receiver on the unit. MFi via I²C chip / CH341, a key file, or a remote signer. The release APK bundles a Carlinkit-extracted key (rejected, D8).
- **Verdict:** a Linux x64 port is feasible (remaining seams: libusb iPhone host, MFi I²C, GStreamer/FFmpeg sink, BlueZ/hostapd, new controller) but duplicates LIVI. It can't run on the T7W as shipped.
- **Gotchas:** Maven Central sometimes returns 429, so retry with backoff. Decision numbers D6/D7 collide across open PR branches (74sto0 = LIVI D6; evidence/version-screen = Pioneer D6/D7), so renumber when merging. This branch is based on main, not on the other PRs.
- **Update 03:00Z:** Jonah dropped the T7W/MFi-chip constraint for a laptop and wants the DiPlay identity used. Wrote docs/laptop-carplay-bliss.md (Bliss OS + stock APK, recommended). Decision card is out: Bliss guide vs native Linux receiver vs both.
- **Next:** act on his pick. If native: write the libusb iPhone host + ffmpeg sink + slim controller in prototypes/, loading the key from a gitignored folder.
