# DiPlay audit + Linux x64 port assessment (2026-11-09)

Source: [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay) at commit `615b32c` (2026-10-28), release 0.2.22 (S19).
Method: shallow clone into the sandbox, read as text, static analysis with `tools/port_scope.py`, then a compile-only build of the portable subset ([prototypes/diplay-jvm-core](../prototypes/diplay-jvm-core/README.md)). **No DiPlay code or APK was executed.**

## 1. What it is (CONFIRMED, from the README, notices and source)
- An Android CarPlay **receiver app** for BYD DiLink head units (`com.shihab.diplay`), written in Kotlin. Wired and wireless.
- A fork of [xcertplay](https://github.com/shilapi/xcertplay) (GPL-3.0). Its protocol research is credited to [LIVI](https://github.com/f-io/LIVI) and Showcase. The package names are still `com.shilapi.xcertplay.*`.
- **License:** GPL-3.0. The home/settings UI file (`DiPlayActivity.kt`) is AGPL-3.0-only (adapted from DiAuto). Any port is GPL-3.0.
- **Platform floor:** `minSdk 24` (Android 7.0). The README says Android 6.0 and older are unsupported. It also needs a head unit that permits APK installation.
- **Size:** 1,545 files in the repo. The `shared` protocol module has 409 main sources and 85.7k lines, of which 38.5k are a bundled pure-Java Opus codec (Concentus).

## 2. How it does CarPlay (CONFIRMED, from the source tree)
It is the **whole CarPlay stack on the head unit**. That is the opposite of node-carplay, where the dongle does everything.

| Layer | DiPlay package | Notes |
|---|---|---|
| iAP2 link, identification, CSM auth, location, vehicle status, route guidance | `iap2/*`, `transport/Iap2*` | pure Kotlin (2.5k lines in `iap2/`) |
| AirPlay/CarPlay session: pair-setup (SRP-6a), pair-verify, RTSP, screen + audio streams, HID touch | `airplay/*` | 7k lines, Bouncy Castle crypto |
| Wired transport | `transport/IphoneUsbHost`, `NcmUsbBridge`, `network/CarPlayVpnService`, `Ipv6NcmBridge` | Android USB host: switches the iPhone to its CarPlay USB configuration, runs **NCM Ethernet in userspace**, and injects the IPv6 packets through an Android `VpnService` |
| Wireless transport | `transport/BluetoothRfcomm*`, `network/*` | iAP2 over Bluetooth RFCOMM, then Wi-Fi Direct, the car hotspot or the same LAN, plus Bonjour (JmDNS) |
| Media | `media/AndroidMediaSink` | `MediaCodec` H.264/HEVC to a `Surface`, `AudioTrack`, microphone uplink with Opus and Speex AEC (JNI) |

### MFi authentication: the part that matters
The `mfi/MfiAuthenticator` interface has three kinds of backend:
1. **A real Apple MFi coprocessor over I²C**, either through `/dev/i2c-N` (`LinuxI2cTransport`, JNI) or through a **CH341 USB-to-I²C adapter** (`Ch341UsbHost`). This follows LIVI's register sequence.
2. **A local key file** (`LocalMfiAuthenticationClient`: `offline-mfi/identity.pk8` + `certificate.p7b`). **The public release APK bundles an accessory certificate and private key recovered from Carlinkit C2Air (Allwinner V821) firmware** (README, THIRD_PARTY_NOTICES). The source tree omits it, and source builds can't start standalone CarPlay without one.
3. **A remote HTTP signer** (`RemoteMfiAuthenticationClient`).

So "no dongle required" means the APK signs with **someone else's Apple-issued identity**. The README itself says acceptance after future iOS updates is unresolved, and the identity is extractable. **We reject that option** (decision D8; same reasoning as the open PR's D6 on xcertplay key-file mode). The legitimate DiPlay configuration for us is option 1: a real MFi chip.

## 3. Comparison
| | node-carplay / react-carplay (current main line) | LIVI | DiPlay |
|---|---|---|---|
| Who speaks CarPlay | the Carlinkit dongle | the host | the host |
| Apple auth | inside the stock dongle | MFi chip on I²C, or a reflashed dongle ("LIVI Link") | MFi chip (I²C/CH341), or the **bundled extracted Carlinkit key (rejected)** |
| Host receives | H.264 + PCM over the dongle's USB protocol | the native AirPlay streams | the native AirPlay streams |
| Wired iPhone | no (the phone talks to the dongle) | yes (Linux kernel NCM) | yes (userspace NCM + VpnService) |
| Runs on | Linux / Electron / browser (WebUSB) | Linux, macOS, Android 10+ | Android 7+ only |
| Language / license | TS, MIT | Rust, GPL | Kotlin, GPL-3.0 |
| Extra hardware | ~$40-80 dongle | MFi chip + I²C, or a dongle to reflash | MFi chip + CH341 (~$5 adapter + a sourced chip) |

## 4. Linux x64 port: feasibility (measured)
`tools/port_scope.py` sorts each source file into one of three classes: pure, tainted (depends on an Android file) or direct (uses Android itself).

| `shared/src/main/java` | pure files | pure lines |
|---|---|---|
| as-is | 241 / 409 | 50.4k / 85.7k |
| with a stub for `Log`/`Build`/`Process`/`Xml` and 2 seam rewrites | **276 / 409** | **56.8k / 85.7k** |

Per package, after the stub: `iap2` 100%, `mfi` 100%, `airplay` 43/44 files (only `CarPlayMediaButton`, which uses `android.view.KeyEvent`, is left out), `transport` 17/43, `media` 29/46, `network` 16/49, `orchestration` 10/15.

**Prototype result (CONFIRMED in the sandbox):** those 276 files **compile with 0 errors** on JDK 21 / Kotlin 2.2.10 against our stubs. That is the whole protocol core: iAP2, AirPlay session + crypto, MFi clients, Lockdown pairing and Opus. Compiling is not running. No behavior was tested, because running third-party code was out of scope for this pass.

**What remains, and its Linux replacement** (the effort figures are estimates, not measurements):
| Seam | Linux replacement | Size |
|---|---|---|
| `IphoneUsbHost`, `IphoneCarPlayConfiguration`, `UsbRequestQueue` | libusb (usb4java or FFM): Apple vendor request → CarPlay configuration → iAP2 bulk endpoints | M |
| `NcmUsbBridge`, `Ipv6NcmBridge`, `CarPlayVpnService` | **deleted**: the kernel's `cdc_ncm` gives a real interface, and ordinary sockets bind to its IPv6 link-local address | (−2k lines) |
| `LinuxI2cTransport` JNI / `Ch341UsbHost` | rebuild `linux_i2c_jni.c` for x86_64, or use FFM; CH341 over libusb | S |
| `AndroidMediaSink` (2.7k lines), `MicrophoneUplink` | GStreamer/FFmpeg decode → SDL/Wayland window, PipeWire audio + mic | L |
| touch | evdev/SDL → the existing `AirPlayHid` encoder | S |
| wireless (`network/*`, BT RFCOMM) | BlueZ RFCOMM, hostapd/NetworkManager AP, JmDNS (already portable) | L |
| `CarPlayController` (3.3k lines, Android services) | a new, slim desktop controller | M |

**Verdict:** technically feasible. Wired-only needs USB + MFi + media + controller, which is a few thousand new lines. **But it duplicates LIVI**, which is already a native Linux receiver built on the same protocol research and the same MFi-chip requirement. On x64 the port buys a JVM/Kotlin codebase and nothing new functionally.

## 5. What it means for the T7W
- **Running DiPlay itself on the T7W: blocked, and probably impossible as shipped.** It needs Android ≥ 7.0 and an APK install path. The T7W's OS is UNKNOWN. The comparable 2015-16 Honda Android units we know of run 4.0.4 / 4.2.2 (S1, S3, S13), so **HYPOTHESIS: the T7W is below 7.0**. Its release build also relies on the rejected extracted identity.
- **The useful asset is the pure-JVM core.** It doesn't depend on any Android API level, so a future on-unit app for an old Android could reuse it with its own USB/media seams. Pair it with a **real MFi chip on a CH341 adapter in the second USB port**, and you'd get dongle-free, true-wired CarPlay with factory touch. HYPOTHESIS, with unverified steps: the old Dalvik/ART would need Java 8+ API desugaring; the unit must allow USB host access to two devices; a Tegra-class SoC must decode CarPlay's H.264 at 800×480 (likely, HEVC unlikely); and root or an install path comes first anyway.
- **Recommendation:** no change to the main line (node-carplay + stock dongle, which needs no MFi chip and no protocol work). Keep LIVI for true-wired Linux tests. Treat DiPlay's core as **the reference library for a dongle-free on-unit receiver**, which only matters after root and only with a real MFi chip.

## 6. Reproduce
```bash
git clone --depth 1 https://github.com/shihabal3amri/DiPlay ~/src/DiPlay
python3 -I tools/port_scope.py ~/src/DiPlay/shared/src/main/java --shim \
  --seam-dir prototypes/diplay-jvm-core/seams --json /tmp/diplay-scope.json
cd prototypes/diplay-jvm-core && DIPLAY_DIR=~/src/DiPlay gradle --no-daemon compileKotlin compileJava
```
