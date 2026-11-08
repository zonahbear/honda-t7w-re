# Session log

## 2026-10-21 (session 1, cloud sandbox)
- Initialized repo, CLAUDE.md, docs, tools, and tests (11 passing).
- Recorded the T7W identifiers Jonah read from the diag menu.
- Research: found S1 (carlinkit-honda-connect), a working CarPlay/AA setup on a **2016 HR-V EX-L** with a Fujitsu Ten MY15ADA unit on Android 4.0.4. Market probably Brazil (HYPOTHESIS).
- Confirmed locally that OpenDroidAuto's bundled HR-V libs are ARMv7 with NVIDIA OMX / Tegra strings (S2).
- Collected Honda dealer update mechanics (Display Repro, Firmware Downloader, USB devices) from NHTSA bulletins.
- Blocked: hrvforum, fitfreak and xda can't be reached from the sandbox. No T7W firmware found.
- NEXT: in-car fingerprinting (docs/next-steps.md #1).

## 2026-11-08 (wired CarPlay client on Linux, cloud sandbox)
- Surveyed Linux/Android CarPlay receivers (S15-S18). LIVI (f-io) is the only maintained Linux project with native wired CarPlay; it needs an MFi chip on I²C or a LIVI Link.
- Installed LIVI's build deps in the sandbox. The build itself was refused by the sandbox policy (third-party build scripts), so it's documented for CachyOS instead.
- Wrote docs/carplay-client-linux.md, decision D6.
- NEXT: Jonah builds LIVI on CachyOS, tests Android phone (AA) then iPhone (expect auth failure), then picks a chip route.
