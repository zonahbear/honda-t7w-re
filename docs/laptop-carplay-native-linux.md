# Native Linux wired CarPlay receiver on a laptop (2026-11-09)

Jonah's pick: a native Linux receiver built from DiPlay's core, authenticated with DiPlay's bundled identity, on a **Lubuntu** laptop (not the T7W, not an MFi chip). This overrides decision D8 for personal laptop testing. The identity never enters this public repo.

Prototype: [prototypes/diplay-jvm-core](../prototypes/diplay-jvm-core/README.md), app package `com.shilapi.xcertplay.desktop`, on branch `claude/project-thread-t0dqd3`.

## Status (2026-11-09)
**Compiles and produces a runnable Lubuntu launcher; the `--help` path runs. The CarPlay bring-up path is RUNTIME-UNTESTED** — no iPhone and no identity were available in the build sandbox, so nothing exercised the USB, pairing, auth or media code against a phone. Treat first run as debugging, not a finished product.

## What is wired (reuses DiPlay's portable core, GPL-3.0)
- **USB host** (`LinuxIphoneUsb`, usb4java/libusb): finds the Apple device (VID `0x05ac`), sends the CarPlay vendor control request (`0xc0`/`0x52`, index `4`), waits for the iPhone to re-enumerate, claims the USBMUX interface (`ff/fe/02`, bulk `0x04`/`0x85`) and feeds it to DiPlay's `Iap2UsbSession`.
- **Networking:** the Linux kernel's `cdc_ncm` driver binds the iPhone's NCM interface and makes a netdev, so DiPlay's whole userspace NCM + Android `VpnService` layer is dropped. The AirPlay server binds that netdev's IPv6 link-local.
- **Control:** DiPlay's `Iap2UsbMuxHost`, `LockdownPairingClient`, `LockdownCarKitClient`, `Iap2Session` and `Iap2WiredControlClient` run unchanged.
- **Auth:** DiPlay's `LocalMfiAuthenticationClient` loads `offline-mfi/identity.pk8` + `certificate.p7b` from `--key-dir` and self-verifies the key against the certificate. This is where the bundled identity plugs in.
- **AirPlay session:** DiPlay's `AirPlaySession` + `CarPlayMediaEngine` handle pairing, crypto, RTSP and stream setup.
- **Media out** (`FfmpegMediaSink`): H.264/HEVC → `ffplay`, audio (LPCM/AAC/Opus) → `ffplay`. DiPlay's `MediaCodecSupport` (pure) does the AVCC→Annex-B conversion.

## What is NOT wired yet (next work, ranked)
1. **Touch/input.** `ffplay` is a separate window, so there's no path from a laptop click to DiPlay's `AirPlaySession.sendTouch`. A real build needs an in-process video surface (JavaFX/OpenGL decoding via JavaCV/FFmpeg, or an SDL window) that owns both the pixels and the pointer, then maps normalized x/y into `AirPlayContact`. Until then it's view-only.
2. **Microphone uplink** (Siri/calls): `MicrophoneUplink` is Android audio; needs a PulseAudio/PipeWire capture seam.
3. **Audio/video sync and buffering**: the two `ffplay` processes don't share a clock. DiPlay's `AndroidMediaSink` buffering/pacing wasn't ported.
4. **HEVC path** is wired but untried.

## Extract the identity from the APK (needed for `--key-dir`)
The identity lives only inside the release APK, not in DiPlay's source. On Lubuntu:
```bash
sudo apt install apktool            # or: unzip the APK
apktool d -s DiPlay-0.2.22.apk -o diplay-apk
mkdir -p ~/t7w-keys/offline-mfi
cp diplay-apk/assets/offline-mfi/identity.pk8     ~/t7w-keys/offline-mfi/
cp diplay-apk/assets/offline-mfi/certificate.p7b  ~/t7w-keys/offline-mfi/
```
Keep `~/t7w-keys` off any public repo. (This is Carlinkit's Apple-issued identity; Apple can revoke it in any iOS update.)

## Run (Lubuntu)
```bash
sudo apt install openjdk-21-jdk ffmpeg libusb-1.0-0   # Gradle comes from ./gradlew
cd prototypes/diplay-jvm-core
sudo cp 70-t7w-iphone.rules /etc/udev/rules.d/ && sudo udevadm control --reload   # USB access without root
git clone https://github.com/shihabal3amri/DiPlay ~/src/DiPlay
# plug in the iPhone, unlock it, note its cdc_ncm netdev:
ip link                                   # look for a new enp0s20u1-style interface
DIPLAY_DIR=~/src/DiPlay KEY_DIR=~/t7w-keys NCM_IFACE=enp0s20u1 ./run-lubuntu.sh
```
The launcher stops `usbmuxd` first (it would otherwise grab the iPhone), builds, and starts the receiver. Unlock the phone and tap **Trust** when prompted. Logs are plain stdout.

## If it doesn't connect (expected, first time)
- **No Apple device found**: check `lsusb` for `05ac:`; re-seat the cable (a data cable, not charge-only).
- **Could not open / permission**: the udev rule didn't take; re-plug after `udevadm control --reload`, or test once with `sudo`.
- **No USBMUX interface**: the CarPlay vendor request was rejected — unlock the iPhone and tap Trust, then it re-enumerates.
- **No link-local on the interface**: `cdc_ncm` hadn't brought the netdev up; `ip link set <iface> up` and re-run.
- Capture the full stdout log for the next debugging pass.
