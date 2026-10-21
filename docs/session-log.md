# Session log

## 2026-10-21 (session 1, cloud sandbox)
- Initialized repo, CLAUDE.md, docs, tools, and tests (11 passing).
- Recorded the T7W identifiers Jonah read from the diag menu.
- Research: found S1 (carlinkit-honda-connect), a working CarPlay/AA setup on a **2016 HR-V EX-L** with a Fujitsu Ten MY15ADA unit on Android 4.0.4. Market probably Brazil (HYPOTHESIS).
- Confirmed locally that OpenDroidAuto's bundled HR-V libs are ARMv7 with NVIDIA OMX / Tegra strings (S2).
- Collected Honda dealer update mechanics (Display Repro, Firmware Downloader, USB devices) from NHTSA bulletins.
- Blocked: hrvforum, fitfreak and xda can't be reached from the sandbox. No T7W firmware found.
- NEXT: in-car fingerprinting (docs/next-steps.md #1).
- Later the same day: Jonah photographed the diag Version page. It matches the recorded identifiers and was added to docs/evidence/2026-10-21/ with EXIF stripped. The rest of next-steps #1 is still open.
- UI survey (Jonah): no app list, Unknown sources, licenses, System Updates or Display Repro. Install path not exposed in the US UI. A bench-spare unit was proposed.
- S16: EX unit confirmed Pioneer (Honda parts catalog). Pivoted to root access; goal clarified as CarPlay-with-touch (Path 1). Added root-access.md, rewrote carplay-feasibility.md, logged root prior-art S17-S20.
- D8: HDMI input = HondaLink, confirmed display-only (no touchscreen control of the source, S21). Path 2 closed; Path 1 (root) is the only touch route. Added docs/hdmi-hondalink.md.
- Analyzed Jonah's HondaLink-impersonation dongle idea (hdmi-dongle-design.md): touch-on-the-wire UNKNOWN pending a USB capture; Park-lock + iAP2/MFi make it 3 blockers vs Path 1's 1. Not pursued over root.
