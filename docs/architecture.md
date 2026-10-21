# Architecture

## Repository

```
CLAUDE.md                 session index + handoff (read first)
README.md                 human overview
requirements.txt          optional Python deps for deeper analysis (.venv)
scripts/setup_cachyos.sh  installs/validates analysis tools; --check is read-only
tools/inspect_firmware.py read-only triage: hashes, magic scan, entropy, ID strings, zip listing
tools/extract_firmware.py safe unpack of zip/tar/gzip into a NEW dir (traversal/link/size guards)
tests/test_tools.py       stdlib unittest on synthetic inputs
docs/                     research (see CLAUDE.md for the index)
firmware/                 originals, gitignored, catalogued in firmware/README.md
analysis/<sha12>/         per-input outputs, gitignored
```

Data flow: `firmware/<orig>` → (read-only) `inspect_firmware.py` → `analysis/<sha12>/inspect.json` → `extract_firmware.py` / binwalk → `analysis/<sha12>/extracted/` → conclusions go into `docs/*.md` with the sha256 they came from.

## Target system model (what we think the T7W looks like; all HYPOTHESIS until next-steps #1)

```
 ┌──────────── T7W head unit ─────────────────────────────┐
 │  Main SoC (HYP: NVIDIA Tegra, ARMv7)                    │
 │   Android 4.0.4 + Fujitsu Ten DA framework (HYP)        │
 │    ├─ whitelist service  (per-app video/audio rights)   │
 │    ├─ mode/audio-focus, steering keys, ECNC mic          │
 │    └─ OMX H.264 decoder                                   │
 │  System μCom H42F10 (power, keys, CAN gateway?) UNKNOWN │
 │  USB host x2 · HDMI in · 800x480 cap touch               │
 └──────────────────────────────────────────────────────────┘
        │ B-CAN / F-CAN              │ USB
     vehicle                     phone / dongle
```

## Projection path being evaluated (Approach A′, see carplay-feasibility.md)

```
iPhone/Android ──Wi-Fi/BT──► CPC200 dongle ──USB bulk: H.264 + PCM──► app on T7W
                                         ◄── touch, mic PCM, keys ───┘
```
