# Research log, sources & compatibility matrix

How directly each source applies to the US T7W is stated explicitly. Labels: CONFIRMED / STRONGLY SUPPORTED / HYPOTHESIS / UNKNOWN / DISPROVEN.

## Sources

| ID | Source | What it says (short) | Applies to T7W? |
|---|---|---|---|
| S1 | [rsilveira/carlinkit-honda-connect](https://github.com/rsilveira/carlinkit-honda-connect) (cloned 2026-10-21, last commit 2026-09-19 UTC-3) | Developed and tested on a **2016 HR-V EX-L**. The head unit is "Honda Connect / Fujitsu Ten Display Audio (`FUJITSU-TEN MY15ADA`), Android 4.0.4 (API 15)". A Carlinkit CPC200 "Auto Box" (USB `1314:1521`) plus a custom API-15 app gives working CarPlay and AA: video, touch, steering-wheel keys, calls. APKs install from USB port 1 only if named `HondaAppCenter_A1.apk`. Driving-mode video and all-speaker audio need a HondaPermissions whitelist entry. | **Indirect.** Same model and year. The market isn't stated: Portuguese terms (`MULTIMIDIA`), cmhonda.net and UTC-3 commit times point to **Brazil** (HYPOTHESIS). |
| S2 | [MaKi983/OpenDroidAuto](https://github.com/MaKi983/OpenDroidAuto) `ext-libs/armeabi-v7a/system/hrv/*.so` | Android AA receiver for Honda Connect. Its bundled HR-V system libs are ARM EABI5 32-bit. `libstagefright.so` (sha256 `cacaee93cdc51e50…`) contains `OMX.Nvidia.h264.decode` and `media.tegra.*`. Also ships `ada-ext-api.jar` (Fujitsu Ten framework API). | **Indirect**, non-US HR-V. Checked locally with `file` and `strings`. |
| S3 | [librick/ic1101](https://github.com/librick/ic1101) | 10th-gen Civic (2016-21) DA: Tegra 3, Android 4.2.2, Mitsubishi "Andromeda" framework. USB OTA updates (`daupdater`, `SwUpdate.mdt` zip) accept **AOSP test-key** signatures, so arbitrary code execution is possible. | **No**: different supplier and generation. Useful only as a method reference. |
| S4 | [NVIDIA / Honda Connect press release, 2014-10-01](https://www.globenewswire.com/fr/news-release/2014/10/01/1149646/0/en/NVIDIA-Powers-New-Honda-Infotainment-System.html) | Honda Connect (Europe, 2015 Civic/Civic Tourer/CR-V) runs on Tegra with Android 4.0.4. | Europe only. Supports the Tegra/4.0.4 pairing for that family. |
| S5 | [hondanews: 2015 Fit Audio and Telematics](https://hondanews.com/en-US/releases/2015-honda-fit-audio-and-telematics) | US 7" Display Audio: 800×480 capacitive. HDMI input (iPhone 5 via Lightning AV adapter) shows video **only in Park**, and touch can't control the device. HondaLink Next Generation "Launcher" shows Honda-approved phone apps. No OS stated. | US sibling (Fit) of the same DA generation: **HYPOTHESIS** that HR-V EX matches. |
| S6 | [Honda SB 17-049 (NHTSA)](https://static.nhtsa.gov/odi/tsbs/2017/MC-10108888-9999.pdf) | 2016 Civic: diag via Day/Night + Power + MENU → Functional Setup → **Display Repro** + USB update device 07AAU-TBAA1A0 ("do not insert into a computer"). | The menu naming matches T7W ("Functional Setup"); the unit doesn't. |
| S7 | [Honda SB 19-066 (NHTSA)](https://static.nhtsa.gov/odi/tsbs/2019/MC-10163184-0001.pdf) | 2017-19 CR-V: dealer **Honda Firmware Downloader** by VIN → USB1/USB2 → Display Repro. | Shows the dealer update channel. No T7W bulletin found yet. |
| S8 | [Honda SB 15-002 (NHTSA)](https://static.nhtsa.gov/odi/tsbs/2015/SB-10107082-9340.pdf) | 2013-15 Accord/Crosstour/Odyssey nav: DVD update ERATO → SH-BOOT → SH-IPL → SH-NK → Utility → BUuCOM. The version screen has a DBOOT field. | Older platform. Shows that a "DBOOT" field isn't unique to Android units. |
| S9 | [Honda ATS 150402 (NHTSA)](https://static.nhtsa.gov/odi/tsbs/2015/MC-10100041-9340.pdf) | 2014 Civic + 2015 Fit 7" Display Audio intermittent faults: fix is to replace the unit. | The DA generation that HR-V most likely belongs to (HYPOTHESIS). |
| S10 | [hondapartsnow 39100-T7W-A71RM](https://www.hondapartsnow.com/genuine/honda~tuner~black~box~panel~rmd~warranty~panasonic~39100-t7w-a71rm.html) | "Tuner … (Panasonic)", 2016 HR-V **LX**. | Not the EX unit. |
| S11 | eBay listings [174989754915](https://www.ebay.com/itm/174989754915), [256241015433](https://www.ebay.com/itm/256241015433) | 2016-18 HR-V EX touch unit part number 39100-T7W-A013-M1. The reman listing's image alt text says "CVH-7738". | Part number: likely relevant. "CVH-7738" is unverified, and a search found nothing. |
| S12 | [usb.honda.com](https://usb.honda.com/) | USB updates for vehicles whose own "System Updates" feature exports `update_by_usb(.json)`. Lists Odyssey/Accord/Pilot. | **HYPOTHESIS: not applicable.** The 2016 HR-V DA has no known System Updates export. To confirm, check the car's settings menu. |
| S13 | [10thcivicforum "infotainment hacking"](https://www.10thcivicforum.com/threads/infotainment-hacking.12249/) | "2016 USA Honda Civic w/Display Audio / Mitsubishi Electric / Android 4.2.2". On the HondaAppCenter rename trick, one poster says "the 2015s have no problem". | Ambiguous. Weak hint that 2015-era US DA units accept the rename install. |
| S14 | [oemdtc TSB 10162450 (A19-070)](https://oemdtc.com/tsb/10162450/) | Starting point from the brief. | **Could not fetch** (client error). Still unread. |
| S15 | [f-io/LIVI](https://github.com/f-io/LIVI) (cloned 2026-11-08, HEAD `bfbea31`, 2026-11-07) | Rust/Slint/GStreamer CarPlay + AA head unit for Linux/macOS/Android 10+. **Native wired + wireless CarPlay** (`iap2-wired` = usbmux/lockdown). MFi chip required: I²C coprocessor (`carPlayMfiI2cBus`/`carPlayMfiPowerGpio`) or "LIVI Link" (reflashed Carlinkit-class dongle). GPL-3.0-or-later. | Best Linux prototype. See carplay-client-linux.md. |
| S16 | [shilapi/xcertplay](https://github.com/shilapi/xcertplay) (cloned 2026-11-08); DiPlay via [besthub article](https://www.besthub.dev/articles/diplay-open-source-carplay-receiver-for-byd-infotainment-no-dongle-required-3493141e1967) | xcertplay: Android 9+ CarPlay receiver, wired + wireless, MFi chip via CH341 I²C bridge or `/dev/i2c-N`, also key files / remote auth. GPL-3.0. DiPlay (BYD) builds on it with an identity extracted from Carlinkit firmware. | Reference only. Extracted-identity route rejected (D6). |
| S17 | [electric-monk/pycarplay](https://github.com/electric-monk/pycarplay) (last commit 2020-12-05), [linguohua/carplay-receiver](https://github.com/linguohua/carplay-receiver) | Python Linux hosts for the Carlinkit "Autobox" dongle protocol. Phone wired into the dongle; dongle does auth. | Protocol sandbox; same family as S1. |
| S18 | node-CarPlay / react-carplay / [RGreeneRI/pi-carplay](https://github.com/RGreeneRI/pi-carplay) | Node/Pi hosts for the same dongle protocol. | Not cloned. Status unchecked. |

## Blocked / failed research paths (don't retry from the cloud sandbox)

- `hrvforum.com`: robots.txt blocks WebFetch, and the egress proxy refuses curl. Key unread threads: "What's the OS in the NA HR-V's head unit?" (/threads/whats-the-os-in-the-na-hr-vs-head-unit.32866/), "Installing APK Files From USB" (/threads/installing-apk-files-from-usb.20898/), "2016-2018 HR-V Aftermarket Head Unit SUCCESS!".
- `fitfreak.net`: proxy refuses. Key unread: "iroot on Display Audio, installing other apps" (3rd-gen GK Fit ICE subforum), "GK 7" Audio Display Reboots (2015 EX)" (the search engine matched it for "H14M").
- `xdaforums.com`: fetch fails. Key unread: "Honda Connect Android System" (t/3179549), "Honda HR-V Android Media Console" (t/3700108).
- **Workaround:** read these in a normal browser on the CachyOS machine and save them to `docs/sources/` as PDF or text, or run a later session on the local machine.
- Search terms with no useful hits: `"1.E200" Honda`, `"CVH-7738"`, `"39100-T7W" audio unit manufacturer` (returns only resellers).
- Search terms that worked: `"H14M" DBOOT Honda display audio` (fitfreak 2015 EX thread), `"MY15ADA" Fujitsu Ten Honda`, `"HondaAppCenter" US Display Audio`.

## Compatibility matrix

| Platform | Market / years | Supplier | SoC | OS | App install | Update path | Relation to T7W |
|---|---|---|---|---|---|---|---|
| **T7W (target)** | US 2016 HR-V EX | UNKNOWN (HYP: Fujitsu Ten) | UNKNOWN (HYP: Tegra) | UNKNOWN (HYP: Android 4.0.4) | UNKNOWN | UNKNOWN (HYP: dealer USB via Display Repro) | — |
| Honda Connect HR-V (S1, S2) | non-US (HYP: Brazil) 2016 | Fujitsu Ten, "MY15ADA" | Tegra (OMX.Nvidia) | Android 4.0.4 | `HondaAppCenter_A1.apk` on USB1 | UNKNOWN | Same model and year, possibly the same hardware |
| Honda Connect EU (S4) | EU 2015 Civic/CR-V | UNKNOWN (HYP: Fujitsu Ten) | Tegra | Android 4.0.4 | Same rename trick (S1 cites XDA) | UNKNOWN | Same family |
| US 7" DA (S5, S9) | US 2014 Civic, 2015 Fit | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | Probable same US generation |
| 10th-gen Civic DA (S3, S13) | US 2016-21 Civic | Mitsubishi Electric | Tegra 3 | Android 4.2.2 | Rename trick partly works | USB OTA, AOAP test key | Different unit |
| 2013-15 Accord/Odyssey nav (S8) | US | UNKNOWN | Renesas SH (from SH-BOOT/NK naming) | WinCE-like (HYP) | No | DVD | Older generation |
| HR-V LX radio (S10) | US 2016 | Panasonic | UNKNOWN | UNKNOWN | No | UNKNOWN | Different trim, different unit |
