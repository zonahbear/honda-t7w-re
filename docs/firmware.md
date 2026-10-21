# Firmware acquisition & format

## Status (2026-10-21): no T7W firmware acquired

| Channel | Status | Notes |
|---|---|---|
| usb.honda.com (owner USB updates) | **HYPOTHESIS: not applicable** | Needs an `update_by_usb.json` exported by the car's System Updates feature, and the site lists newer Odyssey/Accord/Pilot (S12). Check whether the HR-V settings menu has "System Updates"; if not, this channel is closed. |
| Dealer: Honda Firmware Downloader + Display Repro | **UNKNOWN** if any 2016 HR-V DA package exists | Mechanism documented for 2017-19 CR-V (S7) and 2016 Civic (S6). Packages are fetched by VIN on dealer systems. |
| Dealer: pre-loaded USB update device (07AAU-…) | **UNKNOWN** for HR-V | 2016 Civic/Accord used them (S6). The bulletins say not to plug them into a PC, which may mean they're protected or the file layout is fragile (HYPOTHESIS). |
| HR-V service bulletin A19-070 (from brief) | **UNREAD** | oemdtc page failed to fetch. Next: find the NHTSA PDF copy. |
| Honda Service Express (paid techinfo subscription for independents) | **UNKNOWN** whether firmware is included | User decision: costs money. |
| Public dumps / archives | **None found** | No T7W or `1.E200` hits. Search terms are in research.md. |
| Honda Connect (non-US) update packages | **UNKNOWN** | Might share a container format if the hardware matches. Not the same firmware, and **never** to be flashed. |
| Dump from the unit itself | **Blocked** | Needs a shell (unknown if possible) or hardware access (not yet allowed). |

Never treat a matching vehicle year as compatibility evidence. Never flash anything.

## Format notes from related platforms (method reference only)

- 10th-gen Civic (S3): USB update = AOSP-style OTA zip (`SwUpdate.mdt`) staged by `/sbin/daupdater`, then applied in recovery. Signature checked, but AOSP test keys are accepted. **Different supplier, so don't assume the same for T7W.**
- 2013-15 nav (S8): DVD with component order ERATO, SH-BOOT, SH-IPL, SH-NK, Utility, BUuCOM.
- Subaru Fujitsu-TEN/Denso-TEN 7" DA (NHTSA 15-205-16R): two FAT32 USB sticks, `*_USB_1st.exe` / `*_USB_2nd.exe` self-extractors, installed via the unit's Diagnosis Mode → Program Update. Same supplier family but a different OEM's software: a **weak** analogy for the two-stage USB pattern only.

## Analysis workflow once a file exists

```bash
sha256sum firmware/<file>                                   # record in firmware/README.md
H=$(sha256sum firmware/<file> | cut -c1-12); mkdir -p analysis/$H
tools/inspect_firmware.py firmware/<file> --json analysis/$H/inspect.json
binwalk firmware/<file> > analysis/$H/binwalk.txt
tools/extract_firmware.py firmware/<file> analysis/$H/extracted --dry-run
tools/extract_firmware.py firmware/<file> analysis/$H/extracted
```
Then work through the brief's list in this order: container, then headers, compression/crypto (entropy), filesystems, ELF arch (`readelf -h`), bootloader, signing, OS, and finally the graphics/USB/audio stacks.

## What an official T7W update file would unlock (2026-10-21)

Jonah asked whether an official Honda update file is worth getting and whether we can RE it here. **Yes — it's the single highest-value artifact in the project.** One image settles most of the open unknowns at once, all via **read-only, offline** analysis (no flashing).

### Acquisition path (how Jonah might get one)
- **Dealer "Honda Firmware Downloader" by VIN** (the channel documented for 2017-19 CR-V, S7) — a dealer/tech pulls a VIN-specific package. Best shot at the *exact* T7W image.
- **Pre-made dealer USB update device** (07AAU-… style, S6) — physical; Honda says "don't insert into a computer", which may mean a fragile/guarded layout.
- **Honda Service Express** (paid independent subscription) — may expose the package; costs money (Jonah's call).
- Likely **format** for a Pioneer Android unit: an AOSP-style **OTA ZIP** (a `SwUpdate`/`.mdt`-type signed zip with `META-INF/com/google/android/updater-script` + `/system` images), by analogy with the sibling Honda Android units (S3, S17). **Not confirmed** for T7W — could be a Pioneer-proprietary container. Either way, `tools/inspect_firmware.py` fingerprints it first.

### What the image answers
| Question (currently) | What the image gives |
|---|---|
| OS? Android or plain Linux? (UNKNOWN) | Presence of `/system`, `build.prop`, `init*.rc`, AOSP strings → **Android, definitively**; a kernel + busybox rootfs with no Android → plain Linux. |
| Android/kernel version, SoC (UNKNOWN) | `build.prop` (`ro.build.version.*`, fingerprint), `uname`/kernel strings, driver/`OMX.*` names → exact version + chip family. |
| USB-touch / HondaLink question (UNKNOWN) | The HondaLink/iAP service binaries and configs — reading them answers "does it emit touch?" **for free**, settling the dongle idea without a USB capture. |
| Existing CarPlay/AA/projection code (UNKNOWN) | Grep for CarPlay/iAP2/AOAP/projection libraries already present → tells us how much is already there to reuse. |
| Can we even repack+reflash? (UNKNOWN) | Signature/encryption on the container (entropy + header analysis; does it accept AOSP test keys like S3, or is it RSA-signed/encrypted?) → decides whether a *modified* image is installable at all. |
| App-permission / whitelist model | The equivalent of S1's HondaPermissions, if present, for the Pioneer unit. |

### Tooling readiness (CONFIRMED today)
- `tools/inspect_firmware.py` and `tools/extract_firmware.py` run; 11 tests pass. On a synthetic Android-style zip, inspect flagged the `META-INF/com/google/android` OTA path and the `build.prop` / `ro.build.version` strings.
- Workflow is the "Analysis workflow" block above (hash → inspect → binwalk → safe extract → readelf/strings). `binwalk`, `squashfs-tools`, `e2fsprogs`, `android-tools` (simg2img), Ghidra are in `scripts/setup_cachyos.sh` for the deep dive on CachyOS.

### Safety line (unchanged)
Everything here is **read-only**. We analyze the file; we do **not** flash it or any modified version back without Jonah's explicit approval, ideally on a **bench spare**, with a recovery plan (recovery.md). A downloaded binary is untrusted — analyze it in `firmware/`/`analysis/` (gitignored), never execute it.

### How to hand it over
Jonah can **upload the file to this thread**. If it's large, a link works too. First action on arrival: `sha256sum`, log it in `firmware/README.md`, then `inspect_firmware.py … --json`.
