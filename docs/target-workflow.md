# Target workflow: inject a projection client into the firmware, update over USB

Jonah's trajectory (2026-10-21): firmware is USB-updatable → obtain the official T7W package → analyze it here → **inject a CarPlay client (built or adapted) into the package → re-update over USB.** This doc is the end-to-end plan, with the make-or-break gates called out honestly.

Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN. Safety line at the bottom is non-negotiable.

## Pipeline (stages)
```
 [official pkg]─▶ 1 ingest+fingerprint ─▶ 2 unpack ─▶ 3 analyze ─▶ 4 GATE: signing/verify?
                                                                          │
                      ┌───────────────────────────────────────────────────┘
                      ▼
          5 inject client ─▶ 6 repack ─▶ 7 integrity/verify (offline) ─▶ 8 FLASH (approval, bench spare)
```

### 1. Ingest + fingerprint — READY
`sha256sum` → `firmware/README.md`; `tools/inspect_firmware.py … --json` (hashes, magic, entropy, Honda/Android ID strings, zip listing). Answers: container type, compression, obvious encryption (high entropy), Android vs Linux hints.

### 2. Unpack — READY for zip/tar/gz; binwalk for the rest
`tools/extract_firmware.py` (safe, guarded) for zip/tar/gz; `binwalk` + `unsquashfs`/`debugfs`/`simg2img` for carved filesystems. Preserve the original untouched.

### 3. Analyze — tooling ready; conclusions go in docs
Confirm OS (`/system`, `build.prop`, `init*.rc`, AOSP strings = Android; else Linux), kernel, SoC, the HondaLink/iAP service (answers the touch question), any existing projection libs, and the app/permission model. See [firmware.md](firmware.md).

### 4. ⚠️ THE GATE — is the package signed/encrypted AND is it verified on-device?
**This decides whether the whole "inject + USB update" plan is possible at all.** Determined from the image + the updater/loader binary:
- **Unsigned, or signed but accepts known/test keys** (as a *different* Honda unit did — S3, AOSP test keys): **injection is viable.** Repack and the loader will accept it.
- **Signed with a real Honda/Pioneer key and verified by the loader/recovery:** a modified package is **rejected**. This is a **potential hard stop.** Honest options then:
  - find an **unlocked/engineering update mode** or a loader that skips verification (UNKNOWN if one exists);
  - get **root first** (root-access.md) and write the client to `/system` directly, bypassing the signed-update path entirely — often easier than defeating signing;
  - **bench-level reflash** (write the eMMC/NOR on a spare unit with hardware tools) — needs a bench spare and Tier-4 work;
  - defeating the signature check itself = finding a loader vuln, which may not exist and is a research project of its own.
- **Encrypted package:** must recover/locate the key before anything; may be a hard stop.

**We do not know which case applies until the real image + loader are analyzed.** Everything downstream (stages 5-8) is conditional on this gate.

### 5. Inject the client — design, conditional on the gate
**Reality check on "CarPlay client" (important, STRONGLY SUPPORTED):** a real Apple **CarPlay receiver** needs Apple's licensed CarPlay firmware + an **MFi auth coprocessor**. Firmware access does **not** remove that — you cannot bake a license-free CarPlay *host* into `/system`. So "inject a CarPlay client" realistically means one of:
- **(a) Dongle-receiver app baked in:** add an S1-style app (renders a Carlinkit/CPC200 dongle's H.264 output, forwards factory touch) as a **system app/service**. CarPlay works, but the **dongle** (which holds the licensed Apple part) is still required. This is Path 1, pre-installed via firmware instead of sideloaded.
- **(b) Android Auto, no dongle:** an open AA receiver (OpenDroidAuto/`aasdk`, S2) genuinely runs with no licensed hardware. **If the goal tolerates AA-without-dongle, this is the cleanest native win.** CarPlay without a dongle is not achievable at any firmware layer.
Placement: a system-privileged APK + its native libs (OMX/H.264 decode, USB/libusb), wired to the factory display surface and the touch input layer, plus whatever the Pioneer unit's equivalent of S1's permission whitelist is. Mapped precisely once stage 3 reveals the framework.

### 6. Repack — tool built when the real container is known
A format-specific repacker (re-zip with the right alignment/ordering, rebuild the squashfs/ext image, fix headers/checksums). **Not written yet, on purpose** (project rule D4: no speculative format parsers before a real sample — a wrong guess is dead code). The moment the image lands, this is a short job because the container will be known.

### 7. Integrity/verify (offline) — part of the repack tool
Re-run `inspect_firmware.py` on the candidate; diff file trees vs the original; confirm only intended files changed; recompute every checksum the format carries; dry-run any `updater-script`. Produces a candidate package **without flashing**.

### 8. Flash — ONLY with per-action approval, ideally a bench spare
See safety line. A modified-firmware USB update is the single highest-brick-risk action in the project.

## Current tool status
| Stage | Tool | State |
|---|---|---|
| 1 | `tools/inspect_firmware.py` | ready, tested |
| 2 | `tools/extract_firmware.py` + binwalk/unsquashfs | ready |
| 3 | readelf/strings/Ghidra | ready (CachyOS setup script) |
| 6-7 | repacker + verifier | **to be written when the real container is known** (D4) |

## ⚠️ Safety line (non-negotiable)
- **All analysis and repacking here is offline and read-only.** Producing a candidate package is **not** flashing.
- **Nothing is flashed to Jonah's unit without his explicit, per-action approval** — not "go build it", a specific "flash this file now".
- A **modified-firmware USB update can brick the unit.** The first flash of any modified package goes to a **~$150 bench spare** (`39100-T7W-A013-M1`), not the daily driver, with a recovery plan (recovery.md) and the radio anti-theft code recorded first.
- Treat the downloaded firmware as untrusted: analyze in `firmware/`/`analysis/` (gitignored), never execute it.

## Honest bottom line
The plan is sound **if** the gate (stage 4) comes back favorable. If the package is properly signed and verified, injection-then-USB-update likely won't work without either root (write `/system` directly) or bench-level reflash — so **rooting stays the parallel track**, and the firmware tells us which door is open. CarPlay specifically will still need the dongle for Apple auth; Android Auto is the only no-dongle projection. None of this is known until the real image is on disk.
