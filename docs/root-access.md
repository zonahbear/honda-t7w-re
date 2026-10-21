# Root access on the T7W (US 2016 HR-V EX, Pioneer unit)

Goal this feeds: **CarPlay on the factory screen, with working touch** (see [carplay-feasibility.md](carplay-feasibility.md)). Root is the enabler for the native path.
Safety: everything here is read/observe until a step is explicitly approved. No flashing, no `su` install, no recovery write without Jonah's OK (see [recovery.md](recovery.md)). Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN.

## What changed (2026-10-21)
The EX unit is a **Pioneer** build (S16, CONFIRMED), not the Fujitsu Ten unit of the Brazilian HR-V (S1). That re-points the prior art: the right analogues are the **other US Pioneer-era Honda Android head units** — the 2016 Pilot and 2017 Civic — which are **rooted** today (S17, S18). Android on the US T7W is now STRONGLY SUPPORTED (by family), though the exact version is UNKNOWN and the US UI hides the usual Android tells.

## Known entry points (ranked by effort, all need verification on *this* unit)

### E1 — Hidden diagnostic menu → USB role swap → ADB  (HYPOTHESIS, highest value)
- Pattern (from S18/S20 on sibling units): a button-combo diagnostic menu exposes a **USB "Role" = Host/Device** setting. Switch Host→Device, connect a USB-A↔USB-A cable to a PC, and **ADB** may enumerate. From an ADB shell you can read the system (`getprop`, `mount`, `/proc/cpuinfo`, `/dev/block/*`) with **no writes**.
- On this HR-V the exact combo is UNKNOWN. Candidates to try (Tier 0, observe only): Brightness/Day-Night + Phone + Vol-Power held together (S20); other Honda combos use Day/Night+Power+MENU.
- **Why first:** if ADB comes up read-only, it answers OS, SoC, Android version, flash layout and whether `su` already exists — the whole platform question — without changing anything.
- **Approval:** reading over ADB is Tier 1. Running an exploit or writing is Tier 2+ (approval).

### E2 — DirtyCow (CVE-2016-5195) local root  (HYPOTHESIS)
- The 2016 Pilot one-click tool roots via DirtyCow (S17). It works only if the kernel predates the patch (≈ kernels built before late 2016).
- Needs E1 first (a shell to run it from) and Jonah's approval (it modifies the running system). Whether the T7W kernel is vulnerable is UNKNOWN until we read `uname`/kernel date via E1.

### E3 — USB software update / recovery path  (UNKNOWN)
- Software loader `2.5200.3501.45` + the dealer "Display Repro"/Firmware Downloader channel (S6, S7) is the official update route, but the US UI shows **no** Display Repro entry (hardware.md UI survey), so how this unit updates is open.
- If an update package can be obtained (dealer Firmware Downloader by VIN, S7) its signature scheme can be studied **offline** with `tools/inspect_firmware.py`. S17 notes a sibling's update "signature check mechanism" was worked out; S3 showed a *different* Honda unit accepted AOSP test keys. Neither is proven for Pioneer T7W.
- **No flashing.** Acquisition + offline analysis only, until approved.

### E4 — UART / serial on the board  (UNKNOWN, future / bench-spare)
- Standard RE fallback: a serial console on the PCB often gives a boot log and sometimes a root shell. Needs the unit opened — **bench spare**, not the car (recovery.md Tier 4).

## Immediate plan (no approval needed, all Tier 0-1)
1. Try the diag-menu USB-role path (E1) in the car and see if ADB enumerates on a laptop. Read-only: `getprop ro.build.*`, `uname -a`, `cat /proc/cpuinfo`, `mount`, `ls -l /system/bin/su`. Capture to a text file for `docs/evidence/`.
2. Read the fitfreak "iroot" thread (S19) and the Pilot/Civic guides (S17, S18) locally; note the exact menu combo and exploit each used, and the kernel/Android versions.
3. Record findings here and in hardware.md. **Then** decide with Jonah whether to attempt E2 (Tier 2+).

## Hard unknowns gating root
- Android version + kernel build date (decides if DirtyCow or another public exploit applies).
- Whether ADB can be enabled at all on the Pioneer HR-V, and the exact menu to do it.
- Whether `su`/root is already present (some units ship debuggable).
- Update-package format and signing (for E3).
