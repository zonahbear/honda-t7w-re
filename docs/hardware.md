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
| Device number | 35ZB2132 (Jonah re-confirmed it 2026-10-21; not photographed) |
| VIN prefix | `3CZRU5H5XGM…`. Serial omitted because this repo is public; Jonah has the full VIN. Check digit `X` validates. `3` = built in Mexico, `G` = 2016 model year (standard VIN rules). Plant `M` = Celaya and model code `RU5` = HR-V 2WD are HYPOTHESIS. |
| Display | Factory 7" touchscreen |
| USB | 2 ports, flash-drive media works |
| HDMI input | Present |
| Front panel | Single CD slot with eject and day/night keys above the screen; MENU / BACK hard keys on the left bezel (photo) |
| D-RAM / Flash diag | OK / OK |

Diag menu items: Monitor Check, B-CAN, Unit Check, F-CAN, Audio Check, Version, Guideline Setup, Vehicle Status, Functional Setup, GPS Information.

**UI survey, 2026-10-21 (Jonah, reported in chat, no photos):** the user UI has **no** app list or "Install app" icon, **no** "Unknown sources", **no** open-source licenses page, and **no** "System Updates" entry. Diag → Functional Setup has **no** "Display Repro". All of these are CONFIRMED absent from the menus Jonah checked.
Implication: the S1 install path (app list → Install app, `HondaAppCenter_A1.apk`) **isn't exposed in the US UI**. That's DISPROVEN for T7W *as a menu feature*. It doesn't show the OS isn't Android: a US skin could hide it. The dealer update entry point is also unlike the 2016 Civic / CR-V (S6, S7), so how T7W gets updated is UNKNOWN.

Still to record: the Honda part number on the unit label (`39100-T7W-…`) and photos of the Unit Check sub-pages.

## 2. Platform claims

| Question | Status | Best current answer | Basis |
|---|---|---|---|
| OEM (who built the box) | **CONFIRMED: Pioneer** (US EX) | Pioneer | Honda's own parts catalog for the 2016 HR-V EX lists the display/audio unit `39100-T7W-A01…` as "UNIT ASSY., DISPLAY/AUDIO 1 (RMD) **(PIONEER)**" (S16). This is a **different supplier** from the S1 Fujitsu Ten MY15ADA unit (a non-US HR-V). So the US EX T7W is a Pioneer build; the S1/S2 Fujitsu Ten findings are a weaker analogy than before. (LX trim is Panasonic, S10 — a third, separate unit.) |
| CPU / SoC | **HYPOTHESIS** | A Pioneer-era automotive ARM SoC | S2's Tegra evidence is from the **Fujitsu Ten** HR-V, so it does **not** transfer to the Pioneer US unit. Pioneer Honda DA units of this generation (2016 Pilot, 2017 Civic) are ARM Android devices (S17, S18), but the exact SoC is **UNKNOWN**. A bench-spare teardown settles it. |
| OS | **STRONGLY SUPPORTED: Android** (version UNKNOWN) | Android (Pioneer Honda DA family) | The other US Pioneer-built Honda DA units of 2016-17 — the 2016 Pilot and 2017 Civic — are confirmed Android and have been rooted (S17, S18). The HR-V EX shares that supplier and era, so Android is now the leading hypothesis for the US T7W, **despite** the US UI hiding the Android tells we checked (the UI is a locked-down skin). Exact Android version: **UNKNOWN** (Pilot/Civic were ~4.2.2; HR-V could differ). The fitfreak "iroot on Display Audio" thread (S19) is the direct-on-HR-V/Fit lead, still unread from here. |
| Older Honda SH/WinCE platform | **UNKNOWN** (not ruled out) | The 2013-15 Accord/Odyssey nav units update ERATO / SH-BOOT / SH-IPL / SH-NK from DVD (S8): a Renesas SH + WinCE-style layout. Its version screen also has a "DBOOT" field, so the field name alone tells us nothing about the T7W's OS. |
| 10th-gen Civic platform (Mitsubishi, Tegra 3, Android 4.2.2) | **Not the same unit** (STRONGLY SUPPORTED) | Different supplier and model generation (S3, S13). Its update/signing findings **must not** be assumed to apply to T7W. |
| RAM / flash layout | **UNKNOWN** | Diag self-test says both OK; nothing else is known. |
| Bootloader family | **UNKNOWN** | "DBOOT H14M DA US 3.26" names a boot component but not its family. HYPOTHESIS: "H14M" is a Honda 2014-model platform code and "DA US" means Display Audio, US software. |
| Software loader 2.5200.3501.45 | **UNKNOWN** | Probably the updater component (cf. "Display Repro" path in S6/S7). |
| System μCom H42F10 | **UNKNOWN** | A separate microcontroller (power, CAN, keys), common in Japanese head units. Its MCU family is unknown. |
| Video decode | **HYPOTHESIS** | If it's Tegra, there's hardware H.264 decode through OMX (S2). Projection video is H.264, so this matters a lot. |
| USB host | **CONFIRMED** host for mass storage | Can it host a vendor-class bulk device (Carlinkit) or AOAP (Android Auto)? **UNKNOWN** for T7W; demonstrated on the S1 unit. |
| Touch | **CONFIRMED** capacitive, 800×480 WVGA panel (2015 Fit Display Audio spec, S5); HR-V panel resolution is **HYPOTHESIS** (same generation) |
| HDMI input | **CONFIRMED** present; it's the **HondaLink** input | Honda's 2016 HR-V material: Park-only video, uses USB+HDMI (HondaLink iOS cable kit), and **"does not allow control of the connected device through the touchscreen"**. Fit hands-on agrees: "you have to use the device itself" (S21). No touch-back channel. See [hdmi-hondalink.md](hdmi-hondalink.md). |
| Mic / audio routing | **UNKNOWN** for T7W | S1 reached the mic (with echo cancellation) and exclusive audio through Fujitsu Ten framework services (`ecncservice`, `modemanagement`). |
| LX trim radio | **CONFIRMED** different unit | LX uses a Panasonic tuner (39100-T7W-A71x, S10). Don't mix LX evidence with the EX 7" unit. |

## 3. Discriminating question

**Is the US T7W the same Fujitsu Ten / Tegra / Android 4.0.4 box as S1's HR-V, with US software on top?**
If yes, a working CarPlay/AA path already exists (see the feasibility docs). If no, we're back to firmware acquisition and hardware work.
The cheapest discriminators are observations in the car, listed in [next-steps.md](next-steps.md).
