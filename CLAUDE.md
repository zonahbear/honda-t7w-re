# CLAUDE.md: honda-t7w-re (read this first)

Start-of-session protocol: read this file, then **only** the docs the current task needs. Don't scan the repo, and don't redo research listed as done or blocked. Treat this as an index, not as proof. Evidence lives in `docs/` with source IDs (S1…).
Labels: **CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN**.

## 1. Project identity
- **Objective:** native Apple CarPlay + Android Auto on the factory T7W Display Audio of a US 2016 Honda HR-V EX, keeping the original head unit.
- **Target (CONFIRMED, diag menu):** T7W · app FW `1.E200.21` · DBOOT `H14M DA US 3.26` · SW loader `2.5200.3501.45` · System μCom `H42F10 21.35.12` · device no. `35ZB2132` · 7" touch · 2× USB · HDMI in. Full table: [docs/hardware.md](docs/hardware.md).
- **Unknown:** OS, SoC, supplier, update format, whether apps can run.
- **Dev env:** CachyOS (Arch) on Jonah's machine. Session 1 ran in a cloud sandbox (no hrvforum/fitfreak/xda access).
- **Hardware access:** diag menus + USB only. No disassembly yet.
- **Constraints:** daily-driver car. No flashing, Display Repro, or bootloader work without explicit approval. Keep backup cam, audio and CAN working. Never bypass the Park video lock. Downloaded binaries are untrusted. See [docs/recovery.md](docs/recovery.md).

## 2. Current status
- **Phase:** 1, platform identification (research). Phase 0 (memory/docs) done.
- **Done:** repo, docs, setup script, two read-only tools + 11 passing tests; first research pass (S1-S14).
- **Big lead:** a probably-Brazilian 2016 HR-V EX-L runs a Fujitsu Ten "MY15ADA" Android 4.0.4 / Tegra unit, and [carlinkit-honda-connect](https://github.com/rsilveira/carlinkit-honda-connect) gets working CarPlay + AA on it via a Carlinkit CPC200 dongle + custom app (S1, S2). **Whether the US T7W is the same box is UNKNOWN.**
- **Blockers:** no T7W firmware. US-forum evidence unreadable from the sandbox. The OS question needs observations in the car.
- **Linux prototype (2026-11-08):** main line = stock Carlinkit CCPA-class dongle + node-carplay/react-carplay (S18), the same protocol S1 runs on the HR-V. LIVI (S15) does true wired CarPlay but **needs an MFi chip** (I²C) or a LIVI Link (reflashed dongle). Build deps verified; build/run pending on CachyOS. See [docs/carplay-client-linux.md](docs/carplay-client-linux.md).
- **Next action:** in-car fingerprinting, [docs/next-steps.md](docs/next-steps.md) #1; in parallel, LIVI on CachyOS.

## 3. Repository map
| Path | Purpose |
|---|---|
| `CLAUDE.md` | this index + handoff |
| `README.md` | short human overview |
| `scripts/setup_cachyos.sh` | installs/validates pacman tools + `.venv` (`--check` = read-only) |
| `requirements.txt` | optional venv deps (capstone, pyelftools, pyusb) |
| `tools/inspect_firmware.py` | read-only triage: hashes, magic scan, entropy, Honda/Android ID strings, zip listing → JSON |
| `tools/extract_firmware.py` | safe zip/tar/gzip unpack into a **new** dir. Traversal, link and size guards. `--dry-run` |
| `tests/test_tools.py` | stdlib unittest on synthetic samples |
| `firmware/` | originals (gitignored). Catalogue in `firmware/README.md` |
| `analysis/<sha12>/` | per-input outputs (gitignored) |
| `docs/hardware.md` | identifiers + platform claim table |
| `docs/research.md` | **sources S1-S14**, blocked paths, search terms, **compatibility matrix** |
| `docs/firmware.md` | acquisition channels + status, format notes, analysis workflow |
| `docs/carplay-feasibility.md` / `docs/android-auto-feasibility.md` | approach A/A′/B/C scoring |
| `docs/carplay-client-linux.md` | Linux CarPlay receiver survey + LIVI setup (prototype before the head unit) |
| `docs/architecture.md` | repo data flow + hypothesized T7W model |
| `docs/decisions.md` | decisions D1-D5 + dead ends |
| `docs/recovery.md` | safety rules, risk tiers 0-4 |
| `docs/next-steps.md` | ranked experiments, decision gate, unknowns |
| `docs/session-log.md` | dated history |

## 4. Confirmed findings (short; details + URLs in docs/research.md)
- CONFIRMED: T7W identifiers above (from the vehicle).
- CONFIRMED (S1): carlinkit-hrv works on a 2016 HR-V EX-L Fujitsu Ten MY15ADA, Android 4.0.4. APKs install only if named `HondaAppCenter_A1.apk` (USB port 1). The HondaPermissions whitelist controls driving-mode video and exclusive audio. **Applies to T7W: HYPOTHESIS.**
- STRONGLY SUPPORTED (S2, checked locally): HR-V Honda Connect libs are ARMv7 with `OMX.Nvidia.h264.decode` and `media.tegra.*`.
- CONFIRMED (S5): the US 2015 Fit 7" DA is 800×480 capacitive, and its HDMI input shows video only in Park, with no touch passthrough.
- CONFIRMED (S6, S7): Honda dealer DA updates go through diag → Functional Setup → **Display Repro** + USB (Firmware Downloader by VIN, or a pre-made USB device).
- Not the same unit: 10th-gen Civic (Mitsubishi, Tegra 3, Android 4.2.2, test-key OTA, S3), HR-V LX radio (Panasonic, S10), 2013-15 SH/DVD nav (S8).

## 5. Decisions (full: docs/decisions.md)
- D1: "US T7W = Honda Connect Android" is a lead hypothesis, not a fact. Test it cheaply first.
- D2: pure-native CarPlay rejected (needs Apple MFi + licensed stack). Best path is A′: dongle + own app on the factory OS. HDMI (C) is a fallback only.
- D3: native AA (OpenDroidAuto-style, no dongle) stays a candidate.
- D4: tools stay stdlib-only and read-only. No speculative format parsers.
- D5: canonical repo is github.com/zonahbear/honda-t7w-re (created 2026-10-21).
- D6: Linux CarPlay prototype = LIVI + a real MFi chip (I²C or LIVI Link). No extracted/borrowed accessory identities.

## 6. Commands
```bash
scripts/setup_cachyos.sh --check            # report missing tools, change nothing
scripts/setup_cachyos.sh                    # pacman --needed install + .venv + self-test
python3 -m unittest discover -s tests -v    # 11 tests, stdlib only
sha256sum firmware/<f>; H=$(sha256sum firmware/<f>|cut -c1-12); mkdir -p analysis/$H
tools/inspect_firmware.py firmware/<f> --json analysis/$H/inspect.json
binwalk firmware/<f> > analysis/$H/binwalk.txt
tools/extract_firmware.py firmware/<f> analysis/$H/extracted [--dry-run]
# reproduce S2 SoC evidence:
git clone --depth 1 https://github.com/MaKi983/OpenDroidAuto /tmp/oda
strings /tmp/oda/ext-libs/armeabi-v7a/system/hrv/libstagefright.so | grep -E 'OMX.Nvidia|tegra'
```

## 7. Next steps (detail: docs/next-steps.md)
1. **In-car fingerprint** (Tier 0-1, no approval): photograph diag/settings pages, look for an app list / "Install app" / "Unknown sources" / open-source licenses / Display Repro / System Updates, read the part number, run the empty `HondaAppCenter_A1.apk` listing test (no install). Output: hardware.md statuses flipped to CONFIRMED/DISPROVEN.
2. **Read blocked US-forum threads** locally (list in research.md) and save them to `docs/sources/`. Output: US-specific OS/root/APK evidence.
3. **Firmware channel:** NHTSA copy of A19-070 + 2016 HR-V audio TSBs, a dealer question, and a Service Express decision (**Jonah's call, costs money**).
Then the decision gate in next-steps.md. Any Tier 2+ step (installing an APK, etc.) **needs Jonah's approval**.

## 8. Session breadcrumbs
2026-10-21
- Initialized honda-t7w-re (cloud sandbox). CLAUDE.md, docs, tools, tests (11 OK).
- Recorded the vehicle-verified T7W identifiers.
- Found the S1/S2 lead: a non-US 2016 HR-V Honda Connect is Fujitsu Ten / Tegra / Android 4.0.4, with working CarPlay+AA via dongle + app.
- No T7W firmware found. US forums blocked from the sandbox.
- NEXT: in-car fingerprinting (next-steps #1).
2026-11-08
- Surveyed Linux CarPlay receivers (S15-S18). LIVI = best; MFi chip still mandatory for CarPlay, AA needs none.
- Sandbox: deps installed, LIVI build refused by sandbox policy. Steps for CachyOS in docs/carplay-client-linux.md.
- node-carplay/react-carplay read (S18): S1's protocol is derived from it. Main line for T7W.
- NEXT: buy a CCPA-class dongle, bench it on CachyOS with react-carplay at 800×480 (docs/carplay-client-linux.md).
(Older entries move to docs/session-log.md when this grows.)

## Session handoff
- **CURRENT PHASE:** 1, platform identification
- **LAST COMPLETED:** Phase 0 repo + first research pass + tools/tests
- **FILES MODIFIED:** all files (initial commit)
- **VERIFIED FINDINGS:** T7W identifiers (vehicle); S1 working dongle path on a non-US 2016 HR-V; S2 Tegra/ARMv7 libs; Honda dealer update mechanics (S6/S7)
- **FAILED APPROACHES:** sandbox fetch of hrvforum/fitfreak/xda; exact-string searches for `1.E200`, `CVH-7738`; oemdtc A19-070 page
- **OPEN QUESTIONS:** US T7W OS/SoC/supplier? APK install possible? Any T7W update package? Meaning of H14M / 2.5200.3501.45 / H42F10 / 35ZB2132?
- **NEXT EXACT ACTION:** Jonah does next-steps #1 in the car and drops photos/notes in `docs/evidence/<date>/`; the next session updates hardware.md from them
- **COMMAND TO RESUME:** `cd honda-t7w-re && cat CLAUDE.md docs/next-steps.md && python3 -m unittest discover -s tests`
