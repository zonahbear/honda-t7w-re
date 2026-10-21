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

## Phase 2 experiments (root + CarPlay-with-touch) — added 2026-10-21
Ranked; all start Tier 0-1 (observe / read-only). Detail in [root-access.md](root-access.md) and [carplay-feasibility.md](carplay-feasibility.md).

### P1. Try to enable ADB via the diag menu (E1) — do this first
- In the car, enter the diagnostic menu and look for a **USB "Role" = Host/Device** setting. Switch Host→Device, connect a USB-A↔USB-A cable to a laptop, and run `adb devices`.
- If it enumerates: read-only capture `adb shell getprop ro.build.version.release ro.build.fingerprint; uname -a; cat /proc/cpuinfo; mount; ls -l /system/xbin/su /system/bin/su`. Save to `docs/evidence/<date>/adb.txt`.
- Output: OS, Android version, SoC, kernel date, whether root already exists. This alone decides the whole platform question and whether DirtyCow (E2) could apply.
- **Don't** run any exploit or write anything yet (that's Tier 2+, Jonah approves).

### P2. Read the sibling root guides + the HR-V/Fit lead (no car)
- fitfreak "iroot on Display Audio" (S19), the 2016 Pilot root guide + `jersacct/2016PilotOneClick` (S17), the 2017 Civic developer-mode guide (S18). Save to `docs/sources/`.
- Output: exact menu combos, exploit used, kernel/Android versions on the sibling Pioneer/Mitsubishi units.

### P3. Settle the HDMI touch question (Path 2 fallback)
- In the car: feed the HDMI input (any source), confirm whether it's **Park-only** and whether the touchscreen does anything to the source. Expected (from S5): display-only, no touch. If so, Path 2 is dead for CarPlay-with-touch; record it.

## Status 2026-10-21
- #1 steps 1-3 done: nothing Android-like visible, and no Display Repro (see hardware.md "UI survey"). Step 4 (empty `HondaAppCenter_A1.apk` on USB port 1) is still worth doing: on S1's unit the install path is triggered by the file name, so the US software could still react to it even without an app list.
- The gate below now leans toward the "No app install" branch unless step 4, the forum threads (#2), or an unexpected USB reaction says otherwise.
- New candidate experiment (needs Jonah's OK, costs ~$100-200): buy a **used T7W EX unit** (`39100-T7W-A013-M1` or the matching part number) as a bench spare. Open it to identify the SoC, flash chips and debug headers without touching the car. This is the fastest route to a definite answer on OS and SoC.

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
