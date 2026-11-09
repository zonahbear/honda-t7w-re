# Session log

## 2026-10-21 (session 1, cloud sandbox)
- Initialized repo, CLAUDE.md, docs, tools, and tests (11 passing).
- Recorded the T7W identifiers Jonah read from the diag menu.
- Research: found S1 (carlinkit-honda-connect), a working CarPlay/AA setup on a **2016 HR-V EX-L** with a Fujitsu Ten MY15ADA unit on Android 4.0.4. Market probably Brazil (HYPOTHESIS).
- Confirmed locally that OpenDroidAuto's bundled HR-V libs are ARMv7 with NVIDIA OMX / Tegra strings (S2).
- Collected Honda dealer update mechanics (Display Repro, Firmware Downloader, USB devices) from NHTSA bulletins.
- Blocked: hrvforum, fitfreak and xda can't be reached from the sandbox. No T7W firmware found.
- NEXT: in-car fingerprinting (docs/next-steps.md #1).

## 2026-11-09 (DiPlay thread, cloud sandbox)
- Audited DiPlay (S19): a GPL-3.0 Kotlin full-stack CarPlay receiver for Android 7+. MFi comes from an I²C chip/CH341, or from the Carlinkit-extracted key in the release APK (rejected, D8).
- Added `tools/port_scope.py` (static Android-dependency scoper) + 5 tests (16 total).
- Prototype `prototypes/diplay-jvm-core`: 276 DiPlay files (iAP2, AirPlay, MFi, Opus) compile on JDK 21 with ~70 lines of stubs. Nothing executed.
- Verdict: a Linux x64 port is feasible but duplicates LIVI. It can't run on the T7W as shipped (Android ≥7 needed; T7W OS unknown). Main line unchanged.
