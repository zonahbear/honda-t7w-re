# Next steps & open unknowns

## Top 3 experiments (ranked)

### 1. Fingerprint the US T7W software from the driver's seat (Tier 0-1, no approval needed)
- **Goal:** answer "is this an Android / Fujitsu Ten box with app install?"
- **Prerequisites:** car parked, engine on (or battery charger), phone camera, one FAT32 USB stick.
- **Do:**
  1. Photograph every diag page: Version, every Unit Check sub-page, Functional Setup (is there **Display Repro**?).
  2. Home screen and settings: look for an "Application/Widget List" icon (a circle of 6 squares), an "Install app" button, "Unknown sources", an "Info / Open source licenses" screen (an Android unit lists AOSP/Linux licenses there), and any "System Updates" entry.
  3. Read the label on the unit if visible without removal (part number `39100-T7W-…`), or get it from the purchase/VIN records.
  4. USB stick test: put an **empty text file renamed** `HondaAppCenter_A1.apk` on the stick root, insert it in USB port 1, and see whether an install prompt or listing appears. **Do not install anything.** An empty file can't install; we're only checking whether the unit looks for that name.
- **Expected output:** photos in `docs/evidence/2026-xx-xx/` (or text transcriptions), and `hardware.md` updated with CONFIRMED / DISPROVEN.

### 2. Read the blocked US-forum sources (no car needed)
- **Prerequisites:** a normal browser on the CachyOS machine.
- **Do:** open the threads listed in research.md → "Blocked / failed research paths" and save each as PDF or text into `docs/sources/` (gitignored if large). A local Claude Code session can then summarize them.
- **Expected output:** US-specific evidence on OS, root (fitfreak "iroot" thread) and APK install, with every claim labeled.

### 3. Get the firmware channel answered
- **Do:** (a) find the NHTSA PDF for HR-V bulletin A19-070 and any 2016 HR-V audio TSBs; (b) ask a Honda dealer service desk whether a Display Audio software update exists for VIN-specific 2016 HR-V EX (no work done, just ask); (c) decide whether a Honda Service Express subscription is worth it.
- **Approval:** (c) costs money and is Jonah's call.
- **Expected output:** filled rows in `firmware/README.md` / `docs/firmware.md`.

## Decision gate after #1-#2

- **App install works on T7W** → move to Approach A′/A. Next experiment is a Tier 2 test (**needs Jonah's approval**): install a harmless known APK and check it uninstalls cleanly, then evaluate OpenDroidAuto (AA) and carlinkit-hrv (CarPlay + AA) builds.
- **No app install, but it's Android** → look for rootable paths (the "iroot" thread), update-package signing (needs firmware), or hardware access with a bench spare.
- **Not Android** → firmware acquisition and a hardware plan become the critical path, and Approach B/C get re-scored.

## Prioritized unknowns

1. OS of the US T7W (blocks everything).
2. Whether third-party APKs install (decides A/A′).
3. Supplier/SoC confirmation (part label, licenses screen).
4. USB host behaviour with vendor-class devices (decides the dongle route).
5. Existence and format of any T7W update package.
6. Meaning of "H14M", "2.5200.3501.45", "H42F10", "35ZB2132".
