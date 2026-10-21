# T7W hardware & platform

Evidence labels: **CONFIRMED** · **STRONGLY SUPPORTED** · **HYPOTHESIS** · **UNKNOWN** · **DISPROVEN**.
Source IDs (S1, S2…) are defined in [research.md](research.md#sources).

## 1. Identifiers read from the vehicle (CONFIRMED, diag menu, 2026-10-21)

Photo of the Version page (Application / DBOOT / Software loader / System μCom): [evidence/2026-10-21/version-screen.jpeg](evidence/2026-10-21/version-screen.jpeg). The device number and other rows were transcribed by Jonah and aren't photographed yet.

| Property | Value |
|---|---|
| Vehicle | 2016 Honda HR-V EX, US market |
| Head unit model | T7W |
| Application firmware | 1.E200.21 |
| DBOOT | H14M DA US 3.26 |
| Software loader | 2.5200.3501.45 |
| System μCom | H42F10 21.35.12 |
| Device number | 35ZB2132 |
| Display | Factory 7" touchscreen |
| USB | 2 ports, flash-drive media works |
| HDMI input | Present |
| Front panel | Single CD slot with eject and day/night keys above the screen; MENU / BACK hard keys on the left bezel (photo) |
| D-RAM / Flash diag | OK / OK |

Diag menu items: Monitor Check, B-CAN, Unit Check, F-CAN, Audio Check, Version, Guideline Setup, Vehicle Status, Functional Setup, GPS Information.

Still to record from the car (zero risk, see [next-steps.md](next-steps.md)): the Honda part number on the unit label (`39100-T7W-…`), every sub-page of Unit Check / Version, and whether Functional Setup contains **Display Repro**.

## 2. Platform claims

| Question | Status | Best current answer | Basis |
|---|---|---|---|
| OEM (who built the box) | **HYPOTHESIS** | Fujitsu Ten (now Denso Ten) | A 2016 HR-V EX-L whose market isn't stated (probably Brazil) runs a "FUJITSU-TEN MY15ADA" unit (S1). The T7W name, "DA" in DBOOT, and the same model year are consistent with that but don't prove it. |
| CPU / SoC | **HYPOTHESIS** for T7W; **STRONGLY SUPPORTED** for that HR-V unit | NVIDIA Tegra family, 32-bit ARMv7 | System libs pulled from an HR-V Honda Connect unit (S2) are ARM EABI5, and `libstagefright.so` lists `OMX.Nvidia.h264.decode` and `media.tegra.*` properties. NVIDIA said in 2014 that Honda Connect uses Tegra (S4). Exact Tegra part: **UNKNOWN**. |
| OS | **HYPOTHESIS** for T7W | Android 4.0.4 (API 15) | Same as above: that HR-V unit reports API 15 (S1). The brief says not to assume this for the US T7W, and nothing here contradicts that caution: **no US-unit evidence has been seen yet**. Titles of US-forum threads (fitfreak "iroot on Display Audio, installing other apps"; hrvforum "What's the OS in the NA HR-V's head unit?", "Installing APK Files From USB") suggest US owners have probed this, but those pages could not be fetched from this environment (see research.md). |
| Older Honda SH/WinCE platform | **UNKNOWN** (not ruled out) | The 2013-15 Accord/Odyssey nav units update ERATO / SH-BOOT / SH-IPL / SH-NK from DVD (S8): a Renesas SH + WinCE-style layout. Its version screen also has a "DBOOT" field, so the field name alone tells us nothing about the T7W's OS. |
| 10th-gen Civic platform (Mitsubishi, Tegra 3, Android 4.2.2) | **Not the same unit** (STRONGLY SUPPORTED) | Different supplier and model generation (S3, S13). Its update/signing findings **must not** be assumed to apply to T7W. |
| RAM / flash layout | **UNKNOWN** | Diag self-test says both OK; nothing else is known. |
| Bootloader family | **UNKNOWN** | "DBOOT H14M DA US 3.26" names a boot component but not its family. HYPOTHESIS: "H14M" is a Honda 2014-model platform code and "DA US" means Display Audio, US software. |
| Software loader 2.5200.3501.45 | **UNKNOWN** | Probably the updater component (cf. "Display Repro" path in S6/S7). |
| System μCom H42F10 | **UNKNOWN** | A separate microcontroller (power, CAN, keys), common in Japanese head units. Its MCU family is unknown. |
| Video decode | **HYPOTHESIS** | If it's Tegra, there's hardware H.264 decode through OMX (S2). Projection video is H.264, so this matters a lot. |
| USB host | **CONFIRMED** host for mass storage | Can it host a vendor-class bulk device (Carlinkit) or AOAP (Android Auto)? **UNKNOWN** for T7W; demonstrated on the S1 unit. |
| Touch | **CONFIRMED** capacitive, 800×480 WVGA panel (2015 Fit Display Audio spec, S5); HR-V panel resolution is **HYPOTHESIS** (same generation) |
| HDMI input | **CONFIRMED** present | 2015 Fit docs: shows video only in Park, and touch does not control the source (S5). Same behaviour on HR-V: **HYPOTHESIS**. |
| Mic / audio routing | **UNKNOWN** for T7W | S1 reached the mic (with echo cancellation) and exclusive audio through Fujitsu Ten framework services (`ecncservice`, `modemanagement`). |
| LX trim radio | **CONFIRMED** different unit | LX uses a Panasonic tuner (39100-T7W-A71x, S10). Don't mix LX evidence with the EX 7" unit. |

## 3. Discriminating question

**Is the US T7W the same Fujitsu Ten / Tegra / Android 4.0.4 box as S1's HR-V, with US software on top?**
If yes, a working CarPlay/AA path already exists (see the feasibility docs). If no, we're back to firmware acquisition and hardware work.
The cheapest discriminators are observations in the car, listed in [next-steps.md](next-steps.md).
