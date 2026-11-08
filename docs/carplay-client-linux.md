# Wired CarPlay client (receiver) on Linux (2026-11-08)

Goal: get a **wired** CarPlay session running on an ordinary Linux box first, as the dev/prototype step before anything touches the T7W. Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN. Sources S15-S18 are in [research.md](research.md).

## TL;DR

- **Update after the deep dive below:** for the actual T7W goal, the **stock Carlinkit dongle + node-carplay protocol** is the main line (same protocol S1 runs on a 2016 HR-V, no chip work, no reflash). LIVI is the true-wired Linux option.

- **Best Linux candidate: [LIVI](https://github.com/f-io/LIVI) (S15).** Rust + Slint + GStreamer, GPL-3.0-or-later, actively developed (last commit 2026-11-07). It implements the CarPlay **accessory side natively**, wired and wireless, plus Android Auto. Wired CarPlay runs "directly over the USB cable (no OTG required)" via usbmux/lockdown (CONFIRMED from its README and `native/crates/iap2-wired`).
- **The hard blocker is still the MFi chip.** LIVI's README: the MFi authentication coprocessor "cannot be emulated and … LIVI neither ships nor bypasses" it. "Without one of these, native CarPlay is unavailable." (CONFIRMED, S15). Android Auto needs no chip.
- **Two legit ways to supply the chip for LIVI on Linux:**
  1. a bare **MFi coprocessor on I²C** (`config.json`: `carPlayMfiI2cBus`, default 2; `carPlayMfiPowerGpio`, default 21). Fits a Pi or any board with an I²C header. A desktop PC needs a USB-I²C bridge (HYPOTHESIS: the CH341 route xcertplay uses would also show up as `/dev/i2c-N` on Linux).
  2. a **LIVI Link**: a Carlinkit-class CarPlay dongle **reflashed** with LIVI firmware so its own MFi chip answers over the network (CONFIRMED, `LIVI-LINK.md`). It backs up the stock firmware first and has a "back to vendor firmware" path. This flashes a ~$40 dongle, **not the car**.
- **No-reflash fallback:** stock Carlinkit "Autobox" dongle (`1314:1520/1521`) + an open host app (pycarplay S17, node-CarPlay / react-carplay / pi-carplay S18). The phone talks to the dongle, the dongle does CarPlay + auth, and the Linux host only receives H.264/audio and sends touch. This is the **same protocol the S1 HR-V app uses**, which makes it the most portable to an old Android head unit.
- **Pure software, no chip, no dongle:** nothing legitimate. Some projects (xcertplay's key-file mode, DiPlay S16) load an accessory identity **extracted from Carlinkit firmware**. That's someone else's Apple-issued credential, revocable by any iOS update, and we're not building on it (decision D6).

## What was set up in this session

| Step | Result |
|---|---|
| Clone LIVI, xcertplay, pycarplay (`--depth 1`) | CONFIRMED, done |
| Install LIVI's documented Linux build deps (Ubuntu 24.04 sandbox) | CONFIRMED: GStreamer 1.24.2 (+ base, good, bad, libav, `avdec_h264`, `h264parse`), libudev 255, libinput 1.25, wayland 1.22, xkbcommon 1.6, OpenSSL 3.0.13, fontconfig 2.15, gbm, libseat 0.8, libusb 1.0.27, usbmuxd, i2c-tools |
| `cargo xtask build-native` | **Not run.** The sandbox's policy refused running the downloaded repo's build (it runs third-party build scripts and fetches a pinned beta Rust toolchain). Do it on the CachyOS box, below. |
| Wired session | **Blocked here** regardless: no iPhone, no USB passthrough, no MFi chip in a cloud container. |

## Run it on Jonah's CachyOS box

```bash
# 1. deps (Arch names; LIVI's installer maps apt/dnf/pacman automatically)
sudo pacman -S --needed base-devel git pkgconf cmake openssl \
  gstreamer gst-plugins-base gst-plugins-good gst-plugins-bad gst-libav \
  wayland libxkbcommon fontconfig systemd-libs libinput seatd mesa \
  bluez hostapd dnsmasq iw avahi libpulse usbmuxd i2c-tools fuse3
# rustup: the repo pins its own toolchain in rust-toolchain.toml (a dated beta)

# 2. build
git clone --branch main --single-branch https://github.com/f-io/LIVI.git ~/src/LIVI
cd ~/src/LIVI/native && cargo xtask build-native     # or: cargo xtask package-linux (AppImage)
# (or skip building: release AppImage + scripts/install/install.sh, which also writes LIVI's udev rule + sudoers drop-in. Read it first.)

# 3. smoke test WITHOUT a chip (tells us the USB side works)
#    Android phone over USB  -> wired Android Auto should run end to end (no MFi needed)
#    iPhone over USB         -> expect device detection + pairing, then CarPlay fails at auth
DEBUG=1 ./LIVI*.AppImage 2>&1 | tee livi.log
```

Then add the chip (I²C coprocessor or LIVI Link) and retry the iPhone. Success criterion: CarPlay home screen on the Linux window, touch works, audio plays.

## Why this matters for the T7W

| Question | Status |
|---|---|
| Can LIVI run as-is on the T7W? | **UNKNOWN, probably not.** LIVI's Android build needs **Android 10+**, xcertplay needs **Android 9+**. The sibling US Pioneer Honda units are rooted 4.x-era Android (STRONGLY SUPPORTED from project memory), and S1's Fujitsu Ten unit is 4.0.4. A port to an old ARMv7 Android userland would be real work (Rust std's minimum Android API level vs. T7W's is UNKNOWN). |
| Is the dongle-protocol app (S1 style) a better on-unit fit? | **STRONGLY SUPPORTED.** S1 already runs a Carlinkit-protocol app on API 15. The Linux hosts (S17/S18) let us prototype that exact protocol on a PC first. |
| Does the T7W already contain an MFi chip? | **HYPOTHESIS.** It plays iPods/iPhones over USB, and Apple has required an auth coprocessor for iPod accessories for years. If a rooted T7W exposes it on I²C, it might do iAP2 auth. Whether an older chip is accepted for **CarPlay** is UNKNOWN. Only checkable after root (`i2cdetect -l`, look for a device at 0x10/0x11). Read-only probing only, and with approval. |
| Touch | LIVI and the dongle apps both forward touch as CarPlay HID events. On T7W the open question is still reading the factory touchscreen from a third-party process (see carplay-feasibility.md). |

## Candidate scorecard

| Project | Runs on | Wired CarPlay | Auth source | Fit for us |
|---|---|---|---|---|
| **LIVI** (S15) | Linux x86_64/arm64, macOS, Android 10+ | **Yes, native** | MFi chip on I²C, or LIVI Link (reflashed dongle) | **Best Linux prototype.** Not directly portable to an old head unit. |
| xcertplay (S16) | Android 9+ only | Yes, native | MFi chip via CH341/`/dev/i2c-N`; also key files / remote auth | Reference code only. No Linux build. Skip its key-file mode. |
| DiPlay (S16) | BYD DiLink Android 9+ | Yes | Extracted Carlinkit identity | **Rejected** (borrowed credential). |
| pycarplay / carplay-receiver (S17) | Linux, Python | Phone wired **into the dongle** (older Autobox) | Inside the dongle | Easy protocol sandbox. Stale (2020). |
| node-CarPlay / react-carplay / pi-carplay (S18) | Linux / Pi, Node | Via dongle | Inside the dongle | Maintenance status not checked. Same protocol as S1. |
| OpenAuto / Crankshaft / OpenDroidAuto | Linux / Android | **No CarPlay** | n/a | Android Auto only. |
| UxPlay (AirPlay mirroring) | Linux | No (wireless mirroring, not CarPlay, no touch) | none needed | Not CarPlay. |

## Next exact step

Jonah: on the CachyOS box, build or install LIVI, plug in an **Android phone** first (proves wired AA end to end with zero extra hardware), then an iPhone (proves USB/pairing and shows the auth failure). Then pick a chip route: buy an I²C MFi breakout for a Pi, or reflash a spare Carlinkit into a LIVI Link. **Your call: the chip route costs money, and the LIVI Link route reflashes a dongle (not the car).**

## Deep dive: node-CarPlay + react-carplay (2026-11-08, static read only)

Cloned [rhysmorgan134/node-CarPlay](https://github.com/rhysmorgan134/node-CarPlay) (`670f19e`, 2025-06-08, npm `node-carplay` 4.3.0, **MIT**) and [rhysmorgan134/react-carplay](https://github.com/rhysmorgan134/react-carplay) (`090b43d`, 2026-01-14, v4.0.5, Electron + React). Running them was refused by the sandbox policy (third-party code), so everything below comes from reading the source.

**What they are (CONFIRMED from source/README):**
- `node-carplay` is the **driver**: talks to a stock Carlinkit dongle (`1314:1520` / `1314:1521`) over two USB bulk endpoints, from Node (`node-usb`, needs `libudev-dev`) or straight from Chrome via **WebUSB**. It hands out raw H.264 + PCM and accepts touch/keys/mic.
- `react-carplay` is the **app** on top: Electron UI, WebGL/WebGPU renderer in a worker, mic + camera pick, key bindings, plus **SocketCAN** (show a camera on a CAN bit) and PiMost. Ships AppImages (arm64, armv7l, x64) and a udev rule: `SUBSYSTEM=="usb", ATTR{idVendor}=="1314", ATTR{idProduct}=="152*", MODE="0660", GROUP="plugdev"`.
- Supported dongles: **CPC200-CCPA / CPC200-CCPM / CPC200-Autokit** (the "turn an Android head unit into CarPlay" kind). README: dongles that convert wired→wireless CarPlay for cars that already have CarPlay **will not work**.
- The phone connects to the **dongle** (wireless on CCPA-class units), not to the Linux box. The dongle owns the MFi chip and the whole CarPlay/AA session, so **no chip work on our side**. Android Auto also goes through the same dongle (`PhoneType.AndroidAuto`).

**Protocol (CONFIRMED, `src/modules/messages/*.ts`):**
- 16-byte little-endian header: `magic 0x55AA55AA | payload length | type | type ^ 0xFFFFFFFF`, payload in a separate bulk transfer.
- Main types: Open 0x01, Plugged 0x02, Phase 0x03, Unplugged 0x04, Touch 0x05, VideoData 0x06, AudioData 0x07, Command 0x08, MultiTouch 0x17, BoxSettings 0x19, MediaData 0x2A, SendFile 0x99, HeartBeat 0xAA (every 2 s).
- Start-up: SendFile DPI → Open {width, height, fps, format=5, packetMax=49152, iBoxVersion=2, phoneWorkMode=2} → night mode, LHD/RHD, charge mode, box name, BoxSettings, `wifiEnable`, 2.4/5 GHz, mic source, audio transfer → `wifiConnect` 1 s later. Defaults are **800×640 @ 20 fps, 160 dpi**. The T7W panel is **800×480** (S5 sibling).
- Touch: `action, x, y, flags` as uint32, with x/y **normalised 0..10000**. Mapping factory touch → this message is trivial once we can read touch events at all.
- VideoData: 20-byte sub-header (width, height, flags, length, unknown) + raw H.264 NAL stream. AudioData: decodeType, volume (float), audioType, then PCM16.

**Why this matters (STRONGLY SUPPORTED):** S1's Java library (`it.smg.libs.carlinkit.CarlinkitProtocol`) says it is "derived from rhysmorgan134/node-CarPlay", and its unit tests replay bytes captured from a real `1314:1521` dongle (fw `2022.11.19.1218CHY`) **on a Linux PC**. S1 also ships Linux bench scripts (`tools/test_protocol.py`, `test_handshake.py`, `capture_video.py`, pyusb). So node-carplay → S1 → (hopefully) T7W is one protocol lineage, and the PC bench step is already proven by someone else.

**Verdict vs LIVI:**

| | node-carplay / react-carplay | LIVI |
|---|---|---|
| Extra hardware | stock Carlinkit CCPA-class dongle (~$40-80), **no reflash** | MFi chip on I²C, or a dongle **reflashed** into a LIVI Link |
| Phone link | phone ↔ dongle (wireless) | phone ↔ Linux box over USB (true wired) |
| Port to an old Android head unit | **Easy**: tiny protocol, already ported to Android 4.0.4 by S1 | Hard: Rust core, Android 10+ |
| Maturity | node-CarPlay last commit 2025-06; react-carplay 2026-01 | very active (2026-11-07) |

For the T7W goal, **node-carplay + a stock dongle is the main line**. LIVI is a nice Linux toy and a fallback if the dongle protocol ever dies.

**One dongle can't do both at once:** reflashing it into a LIVI Link replaces the stock firmware that node-carplay talks to (LIVI keeps a backup, so it's reversible). Test node-carplay first.

### Bench test on CachyOS (Jonah)

```bash
# A. zero-install: Chrome/Chromium + WebUSB
#    add the udev rule above, replug the dongle, open the node-CarPlay web example
# B. react-carplay AppImage
#    download the x86_64 AppImage from https://github.com/rhysmorgan134/react-carplay/releases
echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="1314", ATTR{idProduct}=="152*", MODE="0660", GROUP="plugdev"' \
  | sudo tee /etc/udev/rules.d/52-nodecarplay.rules && sudo udevadm control --reload
chmod +x react-carplay-*.AppImage && ./react-carplay-*.AppImage
# C. S1's python bench scripts (closest to what the head unit app does)
git clone https://github.com/rsilveira/carlinkit-honda-connect && cd carlinkit-honda-connect/tools
python3 -m venv .venv && . .venv/bin/activate && pip install -r requirements.txt
python3 test_protocol.py
```

Set width/height to **800×480** to match the car. Success = CarPlay on the PC screen, mouse clicks act as taps, audio plays.
