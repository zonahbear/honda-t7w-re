# Firmware acquisition & format

## Status (2026-10-21): no T7W firmware acquired

| Channel | Status | Notes |
|---|---|---|
| usb.honda.com (owner USB updates) | **HYPOTHESIS: not applicable** | Needs an `update_by_usb.json` exported by the car's System Updates feature, and the site lists newer Odyssey/Accord/Pilot (S12). Check whether the HR-V settings menu has "System Updates"; if not, this channel is closed. |
| Dealer: Honda Firmware Downloader + Display Repro | **UNKNOWN** if any 2016 HR-V DA package exists | Mechanism documented for 2017-19 CR-V (S7) and 2016 Civic (S6). Packages are fetched by VIN on dealer systems. |
| Dealer: pre-loaded USB update device (07AAU-…) | **UNKNOWN** for HR-V | 2016 Civic/Accord used them (S6). The bulletins say not to plug them into a PC, which may mean they're protected or the file layout is fragile (HYPOTHESIS). |
| HR-V service bulletin A19-070 (from brief) | **UNREAD** | oemdtc page failed to fetch. Next: find the NHTSA PDF copy. |
| Honda Service Express (paid techinfo subscription for independents) | **UNKNOWN** whether firmware is included | User decision: costs money. |
| Public dumps / archives | **None found** | No T7W or `1.E200` hits. Search terms are in research.md. |
| Honda Connect (non-US) update packages | **UNKNOWN** | Might share a container format if the hardware matches. Not the same firmware, and **never** to be flashed. |
| Dump from the unit itself | **Blocked** | Needs a shell (unknown if possible) or hardware access (not yet allowed). |

Never treat a matching vehicle year as compatibility evidence. Never flash anything.

## Format notes from related platforms (method reference only)

- 10th-gen Civic (S3): USB update = AOSP-style OTA zip (`SwUpdate.mdt`) staged by `/sbin/daupdater`, then applied in recovery. Signature checked, but AOSP test keys are accepted. **Different supplier, so don't assume the same for T7W.**
- 2013-15 nav (S8): DVD with component order ERATO, SH-BOOT, SH-IPL, SH-NK, Utility, BUuCOM.
- Subaru Fujitsu-TEN/Denso-TEN 7" DA (NHTSA 15-205-16R): two FAT32 USB sticks, `*_USB_1st.exe` / `*_USB_2nd.exe` self-extractors, installed via the unit's Diagnosis Mode → Program Update. Same supplier family but a different OEM's software: a **weak** analogy for the two-stage USB pattern only.

## Analysis workflow once a file exists

```bash
sha256sum firmware/<file>                                   # record in firmware/README.md
H=$(sha256sum firmware/<file> | cut -c1-12); mkdir -p analysis/$H
tools/inspect_firmware.py firmware/<file> --json analysis/$H/inspect.json
binwalk firmware/<file> > analysis/$H/binwalk.txt
tools/extract_firmware.py firmware/<file> analysis/$H/extracted --dry-run
tools/extract_firmware.py firmware/<file> analysis/$H/extracted
```
Then work through the brief's list in this order: container, then headers, compression/crypto (entropy), filesystems, ELF arch (`readelf -h`), bootloader, signing, OS, and finally the graphics/USB/audio stacks.
