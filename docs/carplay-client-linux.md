# Wired CarPlay client (receiver) on Linux (2026-11-08)

Goal: get a **wired** CarPlay session running on an ordinary Linux box first, as the dev/prototype step before anything touches the T7W. Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN. Sources S15-S18 are in [research.md](research.md).

## TL;DR

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
